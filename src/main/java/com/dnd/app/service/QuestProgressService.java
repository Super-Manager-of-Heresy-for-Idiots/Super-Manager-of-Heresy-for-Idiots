package com.dnd.app.service;

import com.dnd.app.domain.CharacterQuest;
import com.dnd.app.domain.CharacterQuestObjectiveProgress;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.QuestObjective;
import com.dnd.app.domain.enums.CharacterQuestStatus;
import com.dnd.app.domain.enums.ObjectiveType;
import com.dnd.app.dto.response.ObjectiveProgressResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterQuestObjectiveProgressRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.QuestObjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс QuestProgressService описывает вычисление и обновление прогресса персонажа по
 * опциональным целям квеста (WORLD_PLAN Этап 3). COLLECT_ITEM считается автоматически по
 * инвентарю; прогресс остальных типов хранится в строках прогресса (выставляет мастер).
 * Авторизацию проверяет вызывающий код (CharacterQuestService / QuestService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestProgressService {

    private final QuestObjectiveRepository objectiveRepository;
    private final CharacterQuestObjectiveProgressRepository progressRepository;
    private final ItemInstanceRepository itemInstanceRepository;

    /**
     * Прогресс персонажа по всем целям квеста; пустой список, если у квеста нет целей.
     * @param entry запись журнала (персонаж + квест)
     * @return список прогресса по каждой цели в порядке order_index
     */
    public List<ObjectiveProgressResponse> resolveProgress(CharacterQuest entry) {
        List<QuestObjective> objectives = objectiveRepository
                .findByQuestIdOrderByOrderIndexAsc(entry.getQuest().getId());
        if (objectives.isEmpty()) {
            return List.of();
        }
        Map<UUID, CharacterQuestObjectiveProgress> byObjective = progressRepository
                .findByCharacterQuestId(entry.getId()).stream()
                .collect(Collectors.toMap(p -> p.getObjective().getId(), Function.identity()));

        return objectives.stream()
                .map(o -> toProgress(entry, o, byObjective.get(o.getId())))
                .toList();
    }

    /**
     * Все ли цели квеста выполнены персонажем. Квест без целей считается выполнимым (true) —
     * это сохраняет прежнее поведение сдачи для квестов без опциональных целей.
     * @param entry запись журнала (персонаж + квест)
     * @return true, если целей нет или все выполнены
     */
    public boolean allObjectivesComplete(CharacterQuest entry) {
        return resolveProgress(entry).stream().allMatch(p -> Boolean.TRUE.equals(p.getCompleted()));
    }

    /**
     * Выставляет абсолютное значение прогресса персонажа по цели (мастер). Строка создаётся
     * лениво; completed_at ставится/снимается по достижению требуемого количества.
     * @param entry запись журнала (персонаж + квест)
     * @param objective цель квеста
     * @param currentCount новое значение счётчика (>= 0)
     */
    public void upsertProgress(CharacterQuest entry, QuestObjective objective, int currentCount) {
        int required = requiredCount(objective);
        CharacterQuestObjectiveProgress row = progressRepository
                .findByCharacterQuestIdAndObjectiveId(entry.getId(), objective.getId())
                .orElse(null);
        if (row == null) {
            row = CharacterQuestObjectiveProgress.builder()
                    .characterQuest(entry)
                    .objective(objective)
                    .currentCount(currentCount)
                    .build();
        } else {
            row.setCurrentCount(currentCount);
        }
        row.setCompletedAt(currentCount >= required ? Instant.now() : null);
        progressRepository.save(row);
        log.info("Objective progress set: characterQuestId={}, objectiveId={}, count={}/{}",
                entry.getId(), objective.getId(), currentCount, required);
    }

    /**
     * Изымает у персонажа предметы по целям типа COLLECT_ITEM — «принеси N» означает, что предметы
     * передаются квестодателю. Вызывается один раз в момент сдачи квеста. Строки стеков блокируются,
     * чтобы изъятие не разошлось с конкурентной продажей того же стека.
     * @param entry запись журнала (персонаж + квест)
     */
    public void consumeCollectedItems(CharacterQuest entry) {
        UUID characterId = entry.getCharacter().getId();
        for (QuestObjective objective : objectiveRepository
                .findByQuestIdOrderByOrderIndexAsc(entry.getQuest().getId())) {
            if (objective.getObjectiveType() != ObjectiveType.COLLECT_ITEM || objective.getTargetRef() == null) {
                continue;
            }
            int remaining = requiredCount(objective);
            for (ItemInstance candidate : itemInstanceRepository.findByOwnerCharacterId(characterId)) {
                if (remaining <= 0) {
                    break;
                }
                if (candidate.getTemplate() == null
                        || !objective.getTargetRef().equals(candidate.getTemplate().getId())) {
                    continue;
                }
                ItemInstance locked = itemInstanceRepository.findByIdForUpdate(candidate.getId()).orElse(null);
                if (locked == null) {
                    continue;
                }
                int have = locked.getQuantity() != null ? locked.getQuantity() : 1;
                int take = Math.min(have, remaining);
                if (take >= have) {
                    itemInstanceRepository.delete(locked);
                } else {
                    locked.setQuantity(have - take);
                    itemInstanceRepository.save(locked);
                }
                remaining -= take;
            }
            if (remaining > 0) {
                throw new BadRequestException("Not enough quest items to hand over");
            }
            log.info("Quest items handed over: characterQuestId={}, objectiveId={}, count={}",
                    entry.getId(), objective.getId(), requiredCount(objective));
        }
    }

    // --- Private helpers ---

    private ObjectiveProgressResponse toProgress(CharacterQuest entry, QuestObjective objective,
                                                 CharacterQuestObjectiveProgress row) {
        int required = requiredCount(objective);
        int current;
        if (objective.getObjectiveType() == ObjectiveType.COLLECT_ITEM) {
            // После сдачи предметы уже переданы квестодателю, поэтому считаем цель выполненной,
            // иначе в журнале/очереди подтверждения она выглядела бы как откатившаяся к 0.
            current = isHandedOver(entry) ? required
                    : countInInventory(entry.getCharacter().getId(), objective.getTargetRef());
        } else {
            current = row != null && row.getCurrentCount() != null ? row.getCurrentCount() : 0;
        }
        return ObjectiveProgressResponse.builder()
                .objectiveId(objective.getId())
                .objectiveType(objective.getObjectiveType().name())
                .targetLabel(objective.getTargetLabel())
                .requiredCount(required)
                .currentCount(current)
                .completed(current >= required)
                .build();
    }

    private int countInInventory(UUID characterId, UUID itemTemplateId) {
        if (itemTemplateId == null) {
            return 0;
        }
        return itemInstanceRepository.findByOwnerCharacterId(characterId).stream()
                .filter(i -> i.getTemplate() != null && itemTemplateId.equals(i.getTemplate().getId()))
                .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 1)
                .sum();
    }

    /** Квест уже сдан квестодателю (предметы переданы), ждёт подтверждения или завершён. */
    private boolean isHandedOver(CharacterQuest entry) {
        return entry.getStatus() == CharacterQuestStatus.READY_FOR_TURN_IN
                || entry.getStatus() == CharacterQuestStatus.COMPLETED;
    }

    private int requiredCount(QuestObjective objective) {
        return objective.getRequiredCount() != null && objective.getRequiredCount() > 0
                ? objective.getRequiredCount() : 1;
    }
}
