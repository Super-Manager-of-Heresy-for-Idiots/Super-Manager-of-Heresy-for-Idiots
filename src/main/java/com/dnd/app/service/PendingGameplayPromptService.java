package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureTrigger;
import com.dnd.app.domain.featurerule.PendingGameplayPrompt;
import com.dnd.app.dto.featurerule.PendingPromptResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureTriggerRepository;
import com.dnd.app.repository.PendingGameplayPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс PendingGameplayPromptService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class PendingGameplayPromptService {

    private final PendingGameplayPromptRepository promptRepository;
    private final FeatureTriggerRepository triggerRepository;
    private final GameplayEventService gameplayEventService;
    private final CombatActionEconomyService economyService;

    /**
     * Возвращает список для операции "list pending" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<PendingPromptResponse> listPending(UUID characterId) {
        return promptRepository.findByCharacterIdAndStatus(characterId, "pending").stream()
                .map(this::toResponse).toList();
    }

    /**
     * Выполняет операции "resolve" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param promptId идентификатор prompt, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public PendingPromptResponse resolve(UUID characterId, UUID promptId) {
        PendingGameplayPrompt prompt = require(characterId, promptId);
        if (!"pending".equals(prompt.getStatus())) {
            throw new BadRequestException("Промпт уже обработан");
        }
        // consume the trigger's reaction/resource cost
        if (prompt.getFeatureTriggerId() != null) {
            FeatureTrigger trigger = triggerRepository.findById(prompt.getFeatureTriggerId()).orElse(null);
            if (trigger != null) {
                // A reaction trigger costs the character's one reaction for the round. Spend it FIRST:
                // if it is already used this throws and the resolve rolls back (RAW "one reaction per
                // round"), so the resource below is not consumed either.
                if (trigger.isConsumesReaction() && prompt.getCombatId() != null) {
                    economyService.spend(prompt.getCombatId(), characterId, "reaction");
                }
                gameplayEventService.spendResource(characterId, trigger.getConsumesResourceDefinitionId());
            }
        }
        prompt.setStatus("resolved");
        prompt.setResolvedAt(Instant.now());
        return toResponse(promptRepository.save(prompt));
    }

    /**
     * Выполняет операции "decline" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param promptId идентификатор prompt, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public PendingPromptResponse decline(UUID characterId, UUID promptId) {
        PendingGameplayPrompt prompt = require(characterId, promptId);
        prompt.setStatus("declined");
        prompt.setResolvedAt(Instant.now());
        return toResponse(promptRepository.save(prompt));
    }

    /**
     * Выполняет операции "expire due" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int expireDue() {
        List<PendingGameplayPrompt> due =
                promptRepository.findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore("pending", Instant.now());
        due.forEach(p -> {
            p.setStatus("expired");
            promptRepository.save(p);
        });
        return due.size();
    }

    private PendingGameplayPrompt require(UUID characterId, UUID promptId) {
        PendingGameplayPrompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Промпт не найден: " + promptId));
        if (!characterId.equals(prompt.getCharacterId())) {
            throw new BadRequestException("Промпт не принадлежит этому персонажу");
        }
        return prompt;
    }

    private PendingPromptResponse toResponse(PendingGameplayPrompt p) {
        return PendingPromptResponse.builder()
                .id(p.getId()).combatId(p.getCombatId()).sourceFeatureId(p.getSourceFeatureId())
                .featureTriggerId(p.getFeatureTriggerId()).triggerEventId(p.getTriggerEventId())
                .promptType(p.getPromptType()).status(p.getStatus())
                .expiresAt(p.getExpiresAt()).createdAt(p.getCreatedAt())
                .build();
    }
}
