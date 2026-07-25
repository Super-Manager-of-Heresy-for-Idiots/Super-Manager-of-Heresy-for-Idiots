package com.dnd.app.service;

import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.CampaignQuest;
import com.dnd.app.domain.CharacterQuest;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.QuestNpc;
import com.dnd.app.domain.QuestObjective;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CharacterQuestStatus;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.domain.enums.QuestStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.CharacterQuestResponse;
import com.dnd.app.dto.response.ObjectiveProgressResponse;
import com.dnd.app.dto.response.QuestResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.CharacterQuestRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.QuestNpcRepository;
import com.dnd.app.repository.QuestObjectiveRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.media.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс CharacterQuestService описывает игровой флоу журнала квестов (WORLD_PLAN Этап 2):
 * игрок подходит к NPC-квестодателю (co-presence через {@link PresenceService}),
 * смотрит доступные квесты, берёт квест, может отказаться; сдача — через существующий
 * ГМ-флоу {@code QuestService.completeQuest}, который помечает запись журнала.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterQuestService {

    private final CharacterQuestRepository characterQuestRepository;
    private final QuestNpcRepository questNpcRepository;
    private final CampaignNpcRepository npcRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final PresenceService presenceService;
    private final WebSocketEventService webSocketEventService;
    private final MediaUrlResolver mediaUrlResolver;
    private final QuestService questService;
    private final QuestProgressService questProgressService;
    private final QuestObjectiveRepository questObjectiveRepository;

    /**
     * Возвращает квесты, которые персонаж может взять у этого NPC: связанные через
     * quest_npcs, активные, видимые игрокам и ещё не находящиеся в журнале персонажа.
     * ГМ (без characterId) видит все связанные квесты без фильтров.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-квестодателя
     * @param characterId персонаж игрока (обязателен для не-ГМ)
     * @param username имя пользователя, выполняющего запрос
     * @return список доступных квестов
     */
    @Transactional(readOnly = true)
    public List<QuestResponse> listAvailableQuests(UUID campaignId, UUID npcId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        PlayerCharacter character = null;
        if (characterId != null) {
            character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        } else if (!gm) {
            throw new BadRequestException("characterId is required to browse an NPC's quests");
        }

        if (!gm) {
            requireNpcVisible(npc);
            presenceService.assertSameLocation(character, npc);
        }

        final PlayerCharacter forCharacter = character;
        return questNpcRepository.findByNpcId(npcId).stream()
                .map(QuestNpc::getQuest)
                .filter(Objects::nonNull)
                .filter(q -> gm || (q.getStatus() == QuestStatus.ACTIVE && Boolean.TRUE.equals(q.getIsVisibleToPlayers())))
                .filter(q -> forCharacter == null || isAcceptable(forCharacter.getId(), q.getId()))
                .map(this::toQuestResponse)
                .toList();
    }

    /**
     * Персонаж берёт квест у NPC. Требования: co-presence с NPC (для игрока), квест
     * связан с NPC, активен и видим; в журнале нет активной/завершённой записи.
     * Повторное взятие после отказа реактивирует существующую строку журнала.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-квестодателя
     * @param questId идентификатор квеста
     * @param characterId идентификатор персонажа
     * @param username имя пользователя, выполняющего действие
     * @return запись журнала квестов
     */
    @Transactional
    public CharacterQuestResponse acceptQuest(UUID campaignId, UUID npcId, UUID questId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        if (!gm) {
            requireNpcVisible(npc);
            presenceService.assertSameLocation(character, npc);
        }

        CampaignQuest quest = questNpcRepository.findByNpcId(npcId).stream()
                .map(QuestNpc::getQuest)
                .filter(q -> q != null && q.getId().equals(questId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("This NPC does not offer that quest"));

        if (!gm && (quest.getStatus() != QuestStatus.ACTIVE || !Boolean.TRUE.equals(quest.getIsVisibleToPlayers()))) {
            throw new BadRequestException("This quest is not available");
        }

        CharacterQuest entry = characterQuestRepository
                .findByCharacterIdAndQuestId(characterId, questId)
                .orElse(null);
        if (entry != null) {
            if (entry.getStatus() != CharacterQuestStatus.ABANDONED) {
                throw new BadRequestException("The character already has this quest in the journal");
            }
            entry.setStatus(CharacterQuestStatus.ACCEPTED);
            entry.setGivenByNpc(npc);
            entry.setCompletedAt(null);
        } else {
            entry = CharacterQuest.builder()
                    .character(character)
                    .quest(quest)
                    .givenByNpc(npc)
                    .status(CharacterQuestStatus.ACCEPTED)
                    .build();
        }
        entry = characterQuestRepository.save(entry);

        log.info("Quest accepted: questId={}, characterId={}, npcId={}, by={}", questId, characterId, npcId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_ACCEPTED, campaignId, characterId,
                Map.of("questId", questId, "characterId", characterId, "npcId", npcId,
                        "questTitle", quest.getTitle()), user.getId());
        return toResponse(entry);
    }

    /**
     * Персонаж отказывается от взятого квеста (запись остаётся со статусом ABANDONED).
     * @param campaignId идентификатор кампании
     * @param characterId идентификатор персонажа
     * @param questId идентификатор квеста
     * @param username имя пользователя, выполняющего действие
     */
    @Transactional
    public void abandonQuest(UUID campaignId, UUID characterId, UUID questId, String username) {
        User user = getUser(username);
        boolean gm = isGmOrAdmin(campaignId, user);
        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        campaignService.enforceMembershipOrAdmin(character.getCampaign(), user);

        CharacterQuest entry = characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest is not in this character's journal"));
        if (entry.getStatus() != CharacterQuestStatus.ACCEPTED) {
            throw new BadRequestException("Only an accepted quest can be abandoned");
        }

        entry.setStatus(CharacterQuestStatus.ABANDONED);
        characterQuestRepository.save(entry);

        log.info("Quest abandoned: questId={}, characterId={}, by={}", questId, characterId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_ABANDONED, campaignId, characterId,
                Map.of("questId", questId, "characterId", characterId), user.getId());
    }

    /**
     * Возвращает журнал квестов персонажа (владелец или ГМ).
     * @param campaignId идентификатор кампании
     * @param characterId идентификатор персонажа
     * @param username имя пользователя, выполняющего запрос
     * @return записи журнала
     */
    @Transactional(readOnly = true)
    public List<CharacterQuestResponse> getJournal(UUID campaignId, UUID characterId, String username) {
        User user = getUser(username);
        boolean gm = isGmOrAdmin(campaignId, user);
        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        campaignService.enforceMembershipOrAdmin(character.getCampaign(), user);

        return characterQuestRepository.findByCharacterId(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Помечает запись журнала завершённой (вызывается из ГМ-флоу сдачи квеста).
     * Если персонаж не брал квест — журнал не трогаем (награда всё равно выдаётся).
     * @param questId идентификатор квеста
     * @param characterId идентификатор персонажа-получателя награды
     */
    @Transactional
    public void markCompletedIfAccepted(UUID questId, UUID characterId) {
        characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId).ifPresent(entry -> {
            if (entry.getStatus() == CharacterQuestStatus.ACCEPTED) {
                entry.setStatus(CharacterQuestStatus.COMPLETED);
                entry.setCompletedAt(Instant.now());
                characterQuestRepository.save(entry);
            }
        });
    }

    /**
     * Квесты, которые персонаж может сдать этому NPC: взятые в журнал (ACCEPTED) или уже
     * сданные и ждущие подтверждения ГМа (READY_FOR_TURN_IN), при условии что квест связан
     * с NPC через quest_npcs. Используется агрегированным взаимодействием {@code interact}.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-квестодателя
     * @param characterId персонаж игрока (для ГМ без персонажа список пуст)
     * @param username имя пользователя, выполняющего запрос
     * @return записи журнала, доступные к сдаче у этого NPC
     */
    @Transactional(readOnly = true)
    public List<CharacterQuestResponse> listTurnInQuests(UUID campaignId, UUID npcId, UUID characterId, String username) {
        if (characterId == null) {
            return List.of();
        }
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);
        resolveCampaignCharacter(characterId, campaignId, user, gm);

        Set<UUID> npcQuestIds = questNpcRepository.findByNpcId(npcId).stream()
                .map(QuestNpc::getQuest)
                .filter(Objects::nonNull)
                .map(CampaignQuest::getId)
                .collect(Collectors.toSet());
        if (npcQuestIds.isEmpty()) {
            return List.of();
        }

        return characterQuestRepository.findByCharacterId(characterId).stream()
                .filter(e -> e.getQuest() != null && npcQuestIds.contains(e.getQuest().getId()))
                .filter(e -> e.getStatus() == CharacterQuestStatus.ACCEPTED
                        || e.getStatus() == CharacterQuestStatus.READY_FOR_TURN_IN)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Игрок сдаёт взятый квест квестодателю. Требования: co-presence (для игрока), NPC связан
     * с квестом, запись журнала в статусе ACCEPTED. Если {@code autoCompleteOnTurnIn} — награда
     * выдаётся сразу и запись становится COMPLETED; иначе запись переходит в READY_FOR_TURN_IN
     * и ждёт подтверждения ГМа.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-квестодателя
     * @param questId идентификатор квеста
     * @param characterId идентификатор персонажа
     * @param username имя пользователя, выполняющего действие
     * @return обновлённая запись журнала
     */
    @Transactional
    public CharacterQuestResponse turnInQuest(UUID campaignId, UUID npcId, UUID questId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        if (!gm) {
            requireNpcVisible(npc);
            presenceService.assertSameLocation(character, npc);
        }

        CampaignQuest quest = questNpcRepository.findByNpcId(npcId).stream()
                .map(QuestNpc::getQuest)
                .filter(q -> q != null && q.getId().equals(questId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("This NPC does not accept that quest"));

        CharacterQuest entry = characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId)
                .orElseThrow(() -> new BadRequestException("The character has not taken this quest"));
        if (entry.getStatus() != CharacterQuestStatus.ACCEPTED) {
            throw new BadRequestException("Only an accepted quest can be turned in");
        }

        // Опциональные цели: если у квеста они заданы, все должны быть выполнены. Квест без целей
        // сдаётся без ограничений (allObjectivesComplete возвращает true при отсутствии целей).
        if (!questProgressService.allObjectivesComplete(entry)) {
            throw new BadRequestException("Not all quest objectives are complete");
        }

        // Цели «принеси N»: предметы передаются квестодателю именно в момент сдачи.
        questProgressService.consumeCollectedItems(entry);

        boolean auto = Boolean.TRUE.equals(quest.getAutoCompleteOnTurnIn());
        if (auto) {
            questService.distributeRewards(quest, character, null, username);
            entry.setStatus(CharacterQuestStatus.COMPLETED);
            entry.setCompletedAt(Instant.now());
        } else {
            entry.setStatus(CharacterQuestStatus.READY_FOR_TURN_IN);
        }
        entry = characterQuestRepository.save(entry);

        log.info("Quest turned in: questId={}, characterId={}, npcId={}, auto={}, by={}",
                questId, characterId, npcId, auto, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_TURNED_IN, campaignId, characterId,
                Map.of("questId", questId, "characterId", characterId, "npcId", npcId,
                        "status", entry.getStatus().name(), "questTitle", quest.getTitle()), user.getId());
        return toResponse(entry);
    }

    /**
     * ГМ подтверждает сдачу квеста, ждавшего проверки (READY_FOR_TURN_IN): выдаёт награду
     * персонажу и помечает запись журнала COMPLETED.
     * @param campaignId идентификатор кампании
     * @param characterId идентификатор персонажа
     * @param questId идентификатор квеста
     * @param username имя пользователя (ГМ), выполняющего подтверждение
     * @return обновлённая запись журнала
     */
    @Transactional
    public CharacterQuestResponse confirmTurnIn(UUID campaignId, UUID characterId, UUID questId, String username) {
        User user = getUser(username);
        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, true);
        campaignService.enforceGmOrAdmin(character.getCampaign(), user);

        CharacterQuest entry = characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest is not in this character's journal"));
        if (entry.getStatus() != CharacterQuestStatus.READY_FOR_TURN_IN) {
            throw new BadRequestException("This quest is not awaiting turn-in confirmation");
        }

        questService.distributeRewards(entry.getQuest(), character, null, username);
        entry.setStatus(CharacterQuestStatus.COMPLETED);
        entry.setCompletedAt(Instant.now());
        entry = characterQuestRepository.save(entry);

        log.info("Quest turn-in confirmed: questId={}, characterId={}, by={}", questId, characterId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_TURNED_IN, campaignId, characterId,
                Map.of("questId", questId, "characterId", characterId,
                        "status", entry.getStatus().name()), user.getId());
        return toResponse(entry);
    }

    /**
     * Список сдач, ожидающих подтверждения ГМа в кампании (статус READY_FOR_TURN_IN).
     * @param campaignId идентификатор кампании
     * @param username имя пользователя (ГМ), выполняющего запрос
     * @return записи журнала, ожидающие подтверждения
     */
    @Transactional(readOnly = true)
    public List<CharacterQuestResponse> listPendingTurnIns(UUID campaignId, String username) {
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaignService.findCampaign(campaignId), user);
        return characterQuestRepository
                .findByStatusAndQuestCampaignId(CharacterQuestStatus.READY_FOR_TURN_IN, campaignId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Мастер выставляет прогресс персонажа по опциональной цели квеста (WORLD_PLAN Этап 3).
     * Для COLLECT_ITEM прогресс всё равно считается по инвентарю — ручное значение игнорируется
     * при вычислении, но метод остаётся доступен для единообразия API.
     * @param campaignId идентификатор кампании
     * @param characterId идентификатор персонажа
     * @param questId идентификатор квеста
     * @param objectiveId идентификатор цели
     * @param currentCount новое значение счётчика
     * @param username имя пользователя (мастер)
     * @return обновлённая запись журнала с прогрессом по целям
     */
    @Transactional
    public CharacterQuestResponse setObjectiveProgress(UUID campaignId, UUID characterId, UUID questId,
                                                       UUID objectiveId, int currentCount, String username) {
        User user = getUser(username);
        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, true);
        campaignService.enforceGmOrAdmin(character.getCampaign(), user);

        QuestObjective objective = questObjectiveRepository.findById(objectiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest objective not found"));
        if (objective.getQuest() == null || !objective.getQuest().getId().equals(questId)) {
            throw new BadRequestException("Objective does not belong to this quest");
        }

        CharacterQuest entry = characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest is not in this character's journal"));

        questProgressService.upsertProgress(entry, objective, Math.max(0, currentCount));
        log.info("Objective progress updated by GM: questId={}, characterId={}, objectiveId={}, count={}, by={}",
                questId, characterId, objectiveId, currentCount, username);
        return toResponse(entry);
    }

    // --- Private helpers ---

    private boolean isAcceptable(UUID characterId, UUID questId) {
        return characterQuestRepository.findByCharacterIdAndQuestId(characterId, questId)
                .map(entry -> entry.getStatus() == CharacterQuestStatus.ABANDONED)
                .orElse(true);
    }

    private void requireNpcVisible(CampaignNpc npc) {
        if (!Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
    }

    private CampaignNpc findNpcInCampaign(UUID npcId, UUID campaignId) {
        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
        if (npc.getCampaign() == null || !npc.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("NPC not found in this campaign");
        }
        return npc;
    }

    private PlayerCharacter resolveCampaignCharacter(UUID characterId, UUID campaignId, User user, boolean gm) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        if (!gm && (character.getOwner() == null || !character.getOwner().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You can only manage quests of your own characters");
        }
        return character;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private QuestResponse toQuestResponse(CampaignQuest quest) {
        return QuestResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .status(quest.getStatus() != null ? quest.getStatus().name() : null)
                .isVisibleToPlayers(quest.getIsVisibleToPlayers())
                .artUrl(mediaUrlResolver.resolve(MediaOwnerType.QUEST_ART, quest.getId(), null))
                // Контракт фронта: notes/rewards — обязательные массивы (в игровом флоу не нужны).
                .notes(List.of())
                .rewards(List.of())
                .createdAt(quest.getCreatedAt())
                .updatedAt(quest.getUpdatedAt())
                .build();
    }

    private CharacterQuestResponse toResponse(CharacterQuest entry) {
        CampaignQuest quest = entry.getQuest();
        return CharacterQuestResponse.builder()
                .id(entry.getId())
                .questId(quest.getId())
                .characterId(entry.getCharacter() != null ? entry.getCharacter().getId() : null)
                .characterName(entry.getCharacter() != null ? entry.getCharacter().getName() : null)
                .title(quest.getTitle())
                .description(quest.getDescription())
                .questStatus(quest.getStatus() != null ? quest.getStatus().name() : null)
                .status(entry.getStatus().name())
                .givenByNpcId(entry.getGivenByNpc() != null ? entry.getGivenByNpc().getId() : null)
                .givenByNpcName(entry.getGivenByNpc() != null ? entry.getGivenByNpc().getName() : null)
                .acceptedAt(entry.getAcceptedAt())
                .completedAt(entry.getCompletedAt())
                .objectives(resolveObjectivesOrNull(entry))
                .build();
    }

    /** Прогресс по целям квеста для записи журнала; null, если у квеста нет опциональных целей. */
    private List<ObjectiveProgressResponse> resolveObjectivesOrNull(CharacterQuest entry) {
        List<ObjectiveProgressResponse> progress = questProgressService.resolveProgress(entry);
        return progress.isEmpty() ? null : progress;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
