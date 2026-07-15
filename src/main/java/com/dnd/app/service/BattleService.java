package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.ContestType;
import com.dnd.app.domain.enums.CoverType;
import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.combat.ModifierTarget;
import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.ApplyConditionRequest;
import com.dnd.app.dto.request.AdjustActionEconomyRequest;
import com.dnd.app.dto.request.ApplyCombatantHpRequest;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.BattleCastSpellRequest;
import com.dnd.app.dto.request.BattleUseItemRequest;
import com.dnd.app.dto.request.BattleUseAbilityRequest;
import com.dnd.app.dto.request.BulkActionRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.InitiativeOrderRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.MovementRequest;
import com.dnd.app.dto.request.ContestRequest;
import com.dnd.app.dto.request.FallRequest;
import com.dnd.app.dto.request.ForcedMoveRequest;
import com.dnd.app.dto.request.ReadyActionRequest;
import com.dnd.app.dto.request.SpendActionRequest;
import com.dnd.app.dto.request.StandardActionRequest;
import com.dnd.app.dto.request.TeleportRequest;
import com.dnd.app.dto.request.TrapTriggerRequest;
import com.dnd.app.integration.map.MapTokenMover;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.featurerule.AvailableFeatureAction;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.BattleUseAbilityResult;
import com.dnd.app.dto.featurerule.FeatureUseRequest;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.domain.StatType;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.AttackResolver;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.CombatCalculator;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Класс BattleService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    /** Well-known code of the system Dexterity stat (used for initiative). */
    private static final String DEX_CODE = "dex";

    /** Well-known code of the walking movement type; a monster's walk speed drives its movement budget. */
    private static final String WALK_CODE = "walk";

    /** Fallback speed (ft) when a character has no sheet speed or a monster has no walk speed. */
    private static final int DEFAULT_SPEED_FT = 30;
    private static final String USE_ABILITY_COMMAND = "battle.useAbility";

    private final BattleRepository battleRepository;
    private final BattleCombatantRepository combatantRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final MonsterService monsterService;
    private final CharacterService characterService;
    private final CharacterResourceService characterResourceService;
    private final CharacterEffectService characterEffectService;
    private final WebSocketEventService webSocketEventService;
    private final DiceRoller diceRoller;
    private final WeaponAttackService weaponAttackService;
    private final ClassAbilityCombatService classAbilityCombatService;
    private final ItemInstanceRepository itemInstanceRepository;
    private final SpellRepository spellRepository;
    private final SpellSlotService spellSlotService;
    private final ObjectMapper objectMapper;
    private final CharacterHpService characterHpService;
    private final ModifierAggregator modifierAggregator;
    private final EffectExpirationService effectExpirationService;
    private final DamageMitigationService damageMitigationService;
    private final ConditionService conditionService;
    private final BattleLogService battleLogService;
    private final SpellCastService spellCastService;
    private final FeatureUseService featureUseService;
    private final ItemAbilityUseService itemAbilityUseService;
    private final ItemAbilityResolver itemAbilityResolver;
    private final CombatFeatureExecutionService combatFeatureExecutionService;
    private final StatTypeRepository statTypeRepository;
    private final FeatureEffectService featureEffectService;
    private final com.dnd.app.integration.map.MapZoneCreator mapZoneCreator;
    /** Клиент принудительного перемещения токенов на карте (push/pull/slide/телепорт, фаза 2.12). */
    private final com.dnd.app.integration.map.MapTokenMover mapTokenMover;
    /** Идемпотентность боевых команд по clientCommandId — защита от дублей (фаза 2.14). */
    private final CommandDedupService commandDedupService;
    /** Runtime-список доступных классовых умений; field injection оставляет старые unit-конструкторы совместимыми. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FeatureActionService featureActionService;

    // ================================ Lifecycle ================================

    /**
     * Создает результат операции "create battle" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse createBattle(UUID campaignId, CreateBattleRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        campaignService.enforceCampaignActive(campaign);

        Battle battle = Battle.builder()
                .campaign(campaign)
                .name(request != null ? request.getName() : null)
                .status(BattleStatus.ASSEMBLING)
                .createdBy(user)
                .build();
        battle = battleRepository.save(battle);

        log.info("Battle created: id={}, campaignId={}, by={}", battle.getId(), campaignId, username);
        return toResponse(battle, List.of());
    }

    /**
     * Возвращает список для операции "list battles" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BattleResponse> listBattles(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return battleRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(b -> toResponse(b, combatantRepository.findByBattleIdOrderByTurnOrderAsc(b.getId())))
                .toList();
    }

    /**
     * Возвращает результат операции "get battle" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public BattleResponse getBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Возвращает результат операции "get battle log" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param afterSeq граница выборки, используемая для продолжения бизнес-потока
     * @param limit ограничение размера результата бизнес-операции
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BattleLogEntryResponse> getBattleLog(UUID campaignId, UUID battleId, Long afterSeq,
                                                     Integer limit, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        findBattle(battleId, campaignId);
        boolean isGm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        return battleLogService.list(battleId, afterSeq == null ? 0L : afterSeq,
                limit == null ? 0 : limit, isGm);
    }

    /**
     * Выполняет операции "end battle" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse endBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);

        if (battle.getStatus() == BattleStatus.COMPLETED) {
            throw new BadRequestException("Battle is already completed");
        }
        battle.setStatus(BattleStatus.COMPLETED);
        battle.setEndedAt(Instant.now());
        battleRepository.save(battle);

        log.info("Battle ended: id={}, by={}", battleId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ENDED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    // ============================== Conditions ==============================

    /**
     * Выполняет операции "apply condition" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public List<CombatantConditionResponse> applyCondition(UUID campaignId, UUID battleId, UUID combatantId,
                                                           ApplyConditionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        BattleCombatant combatant = loadCombatantInBattle(battleId, combatantId);
        enforceControls(campaignId, user, combatant);
        List<CombatantConditionResponse> result = conditionService.apply(campaignId, combatant,
                request.getConditionId(), request.getSourceText(), request.getRemainingRounds(),
                user.getId(), battle.getRoundNumber());
        Map<String, Object> condLog = new HashMap<>();
        condLog.put("combatantName", combatant.getDisplayName());
        condLog.put("action", "ADDED");
        condLog.put("conditionId", request.getConditionId());
        result.stream().filter(c -> request.getConditionId().equals(c.getConditionId())).findFirst()
                .ifPresent(c -> condLog.put("code", c.getCode()));
        if (request.getSourceText() != null) {
            condLog.put("source", request.getSourceText());
        }
        if (request.getRemainingRounds() != null) {
            condLog.put("rounds", request.getRemainingRounds());
        }
        // Обратимость (фаза 3.5): откат снимет это состояние с комбатанта.
        Map<String, Object> undo = new HashMap<>();
        undo.put("kind", "CONDITION_ADD");
        undo.put("combatantId", combatant.getId().toString());
        undo.put("conditionId", request.getConditionId().toString());
        battleLogService.append(battleId, campaignId, BattleLogType.CONDITION, null, combatant.getId(),
                condLog, BattleLogVisibility.PUBLIC, user.getId(), undo);
        return result;
    }

    /**
     * Удаляет результат операции "remove condition" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param conditionId идентификатор condition, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public List<CombatantConditionResponse> removeCondition(UUID campaignId, UUID battleId, UUID combatantId,
                                                            UUID conditionId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        findBattle(battleId, campaignId);
        BattleCombatant combatant = loadCombatantInBattle(battleId, combatantId);
        enforceControls(campaignId, user, combatant);
        List<CombatantConditionResponse> result = conditionService.remove(campaignId, combatantId, conditionId, user.getId());
        Map<String, Object> condLog = new HashMap<>();
        condLog.put("combatantName", combatant.getDisplayName());
        condLog.put("action", "REMOVED");
        condLog.put("conditionId", conditionId);
        battleLogService.append(battleId, campaignId, BattleLogType.CONDITION, null, combatantId,
                condLog, BattleLogVisibility.PUBLIC, user.getId());
        return result;
    }

    private BattleCombatant loadCombatantInBattle(UUID battleId, UUID combatantId) {
        return combatantRepository.findById(combatantId)
                .filter(c -> c.getBattle() != null && c.getBattle().getId().equals(battleId))
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));
    }

    /**
     * Выполняет операции "bulk action" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse bulkAction(UUID campaignId, UUID battleId, BulkActionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Bulk actions only happen in an active battle");
        if (request.getCombatantIds() == null || request.getCombatantIds().isEmpty()) {
            throw new BadRequestException("No targets selected");
        }

        List<BattleCombatant> targets = new java.util.ArrayList<>();
        for (UUID id : request.getCombatantIds()) {
            targets.add(combatantRepository.findByIdForUpdate(id)
                    .filter(c -> c.getBattle() != null && c.getBattle().getId().equals(battleId))
                    .orElseThrow(() -> new BadRequestException("Combatant not in battle: " + id)));
        }
        int round = nz(battle.getRoundNumber());
        int affected = 0;

        switch (request.getType()) {
            case DAMAGE -> {
                int base = nz(request.getAmount());
                for (BattleCombatant target : targets) {
                    int dmg = base;
                    if (dmg > 0 && request.getSaveDc() != null && request.getSaveAbility() != null) {
                        int d20 = diceRoller.rollD20();
                        int bonus = resolveTargetSaveBonus(target, request.getSaveAbility());
                        AttackResolver.SaveOutcome save = AttackResolver.resolveSave(d20, bonus, request.getSaveDc());
                        if (save == AttackResolver.SaveOutcome.SUCCESS) {
                            dmg = Boolean.TRUE.equals(request.getHalfOnSave()) ? dmg / 2 : 0;
                        }
                    }
                    if (dmg > 0) {
                        int fin = damageMitigationService.mitigate(target, dmg, request.getDamageTypeId()).finalDamage();
                        if (fin > 0) {
                            applyDamageOrHeal(target, -fin, user, campaignId);
                        }
                    }
                    affected++;
                }
            }
            case HEAL -> {
                int heal = nz(request.getAmount());
                for (BattleCombatant target : targets) {
                    if (heal > 0) {
                        applyDamageOrHeal(target, heal, user, campaignId);
                    }
                    affected++;
                }
            }
            case CONDITION_ADD -> {
                if (request.getConditionId() == null) {
                    throw new BadRequestException("conditionId is required for CONDITION_ADD");
                }
                for (BattleCombatant target : targets) {
                    conditionService.apply(campaignId, target, request.getConditionId(),
                            request.getSourceText(), request.getRemainingRounds(), user.getId(), round);
                    affected++;
                }
            }
            case CONDITION_REMOVE -> {
                if (request.getConditionId() == null) {
                    throw new BadRequestException("conditionId is required for CONDITION_REMOVE");
                }
                for (BattleCombatant target : targets) {
                    conditionService.remove(campaignId, target.getId(), request.getConditionId(), user.getId());
                    affected++;
                }
            }
        }

        Map<String, Object> logPayload = new HashMap<>();
        logPayload.put("action", request.getType().name());
        logPayload.put("count", affected);
        if (request.getAmount() != null) {
            logPayload.put("amount", request.getAmount());
        }
        battleLogService.append(battleId, campaignId, BattleLogType.GM_OVERRIDE, null, null,
                logPayload, BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Bulk action {} on {} combatants: battleId={}, by={}",
                request.getType(), affected, battleId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    // ============================== Death saves ==============================

    /**
     * Выполняет бросок операции "roll death save" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param manualRoll входящее значение manual roll, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse rollDeathSave(UUID campaignId, UUID battleId, UUID combatantId, Integer manualRoll, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .filter(c -> c.getBattle().getId().equals(battleId))
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));
        enforceControls(campaignId, user, combatant);
        PlayerCharacter character = requireDyingCharacter(combatant);

        int d20 = manualRoll != null ? manualRoll : diceRoller.rollD20();
        if (d20 < 1 || d20 > 20) {
            throw new BadRequestException("d20 must be between 1 and 20");
        }

        if (d20 == 20) {
            // Regain consciousness at 1 HP; the heal path clears counters + the unconscious condition.
            applyDamageOrHeal(combatant, 1, user, campaignId);
        } else if (d20 == 1) {
            addDeathSaveFailures(character, 2);
            characterRepository.save(character);
        } else if (d20 >= 10) {
            int successes = Math.min(3, nz(character.getDeathSaveSuccesses()) + 1);
            character.setDeathSaveSuccesses(successes);
            if (successes >= 3) {
                // Stable: stop rolling, clear the counters, but stay unconscious at 0 HP.
                character.setDeathSaveSuccesses(0);
                character.setDeathSaveFailures(0);
            }
            characterRepository.save(character);
        } else {
            addDeathSaveFailures(character, 1);
            characterRepository.save(character);
        }
        // Private pip detail (owner/GM only); a resulting death is public.
        Map<String, Object> rollLog = new HashMap<>();
        rollLog.put("targetName", combatant.getDisplayName());
        rollLog.put("event", "ROLL");
        rollLog.put("d20", d20);
        rollLog.put("successes", nz(character.getDeathSaveSuccesses()));
        rollLog.put("failures", nz(character.getDeathSaveFailures()));
        battleLogService.append(battleId, campaignId, BattleLogType.DEATH_SAVE, null, combatantId,
                rollLog, BattleLogVisibility.GM_ONLY, user.getId());
        if (character.getStatus() == CharacterStatus.DEAD) {
            logDeathSaveEvent(combatant, campaignId, user.getId(), "DEAD", BattleLogVisibility.PUBLIC, null);
        }

        webSocketEventService.sendCampaignEvent(WebSocketEventType.HP_CHANGED, campaignId,
                java.util.Map.of("battleId", battleId, "combatantId", combatantId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Выполняет операции "stabilize" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse stabilize(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .filter(c -> c.getBattle().getId().equals(battleId))
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));
        enforceControls(campaignId, user, combatant);
        PlayerCharacter character = requireDyingCharacter(combatant);
        character.setDeathSaveSuccesses(0);
        character.setDeathSaveFailures(0);
        characterRepository.save(character);
        logDeathSaveEvent(combatant, campaignId, user.getId(), "STABILIZED", BattleLogVisibility.PUBLIC, null);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Выполняет операции "reroll initiative" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse rerollInitiative(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Initiative can only be rerolled in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, combatants);
        BattleCombatant target = combatants.stream()
                .filter(c -> c.getId().equals(combatantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));

        int d20 = diceRoller.rollD20();
        int initiative;
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null) {
            int dex = target.getMonster().getDexScore() != null ? target.getMonster().getDexScore().intValue() : 10;
            Integer bonus = target.getMonster().getInitiativeBonus() != null
                    ? target.getMonster().getInitiativeBonus().intValue() : null;
            initiative = CombatCalculator.monsterInitiative(d20, bonus, dex);
        } else if (target.getCharacter() != null) {
            PlayerCharacter character = target.getCharacter();
            initiative = CombatCalculator.characterInitiative(d20, dexValue(character), dexBuffBonus(character));
        } else {
            initiative = d20;
        }
        target.setInitiativeRoll(d20);
        target.setInitiative(initiative);

        // Re-sort the tracker and keep the turn on the same combatant (mirrors join).
        CombatCalculator.orderTracker(combatants);
        combatantRepository.saveAll(combatants);
        battle.setCurrentTurnIndex(
                CombatCalculator.resolveCurrentIndex(combatants, activeId, battle.getCurrentTurnIndex()));
        battleRepository.save(battle);

        battleLogService.append(battleId, campaignId, BattleLogType.TURN, target.getId(), null,
                Map.of("event", "INITIATIVE_REROLL", "combatantName", target.getDisplayName(),
                        "d20", d20, "initiative", initiative), BattleLogVisibility.PUBLIC, user.getId());
        log.info("Initiative rerolled: battleId={}, combatant={}, d20={}, initiative={}, by={}",
                battleId, target.getDisplayName(), d20, initiative, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    /**
     * Устанавливает результат операции "set initiative order" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param entries входящее значение entries, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse setInitiativeOrder(UUID campaignId, UUID battleId,
            List<InitiativeOrderRequest.Entry> entries, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Initiative can only be reordered in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, combatants);

        // The request must cover exactly the battle's combatants — no missing, extra or duplicate ids.
        Map<UUID, Integer> byId = new HashMap<>();
        for (InitiativeOrderRequest.Entry e : entries) {
            if (byId.put(e.getCombatantId(), e.getInitiative()) != null) {
                throw new BadRequestException("Duplicate combatant in initiative order: " + e.getCombatantId());
            }
        }
        if (byId.size() != combatants.size() || !combatants.stream().allMatch(c -> byId.containsKey(c.getId()))) {
            throw new BadRequestException("Initiative order must list every combatant of the battle exactly once");
        }

        for (BattleCombatant c : combatants) {
            c.setInitiative(byId.get(c.getId()));
            c.setInitiativeRoll(null); // GM-set value, not a rolled d20
        }
        CombatCalculator.orderTracker(combatants);
        combatantRepository.saveAll(combatants);
        battle.setCurrentTurnIndex(
                CombatCalculator.resolveCurrentIndex(combatants, activeId, battle.getCurrentTurnIndex()));
        battleRepository.save(battle);

        battleLogService.append(battleId, campaignId, BattleLogType.TURN, null, null,
                Map.of("event", "INITIATIVE_ORDER_SET"), BattleLogVisibility.PUBLIC, user.getId());
        log.info("Initiative order set: battleId={}, by={}", battleId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    /**
     * Применяет заклинание операции "cast spell" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellCastResult castSpell(UUID campaignId, UUID battleId, BattleCastSpellRequest request, String username) {
        User user = getUser(username);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Spells can only be cast in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        BattleCombatant caster = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, caster);
        if (caster.getType() != CombatantType.CHARACTER || caster.getCharacter() == null) {
            throw new BadRequestException("Only characters can cast spells in battle");
        }
        PlayerCharacter casterChar = caster.getCharacter();

        // A character target maps to its sheet for feature EFFECTS; a monster target has no sheet, so
        // effects fall back to the caster (its damage/healing lands on the combatant below — 2.1b).
        // Validate the target is in this battle.
        PlayerCharacter effectTarget = casterChar;
        UUID targetCombatantId = request.getTargetCombatantId();
        if (targetCombatantId != null) {
            BattleCombatant target = combatants.stream()
                    .filter(c -> c.getId().equals(targetCombatantId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Target does not belong to this battle"));
            if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null) {
                effectTarget = target.getCharacter();
            }
        }

        SpellCastRequest scReq = SpellCastRequest.builder()
                .combatId(battleId)
                .slotLevel(request.getSlotLevel())
                .targetCharacterId(effectTarget != casterChar ? effectTarget.getId() : null)
                .build();
        SpellCastResult result = spellCastService.cast(casterChar, request.getSpellId(), scReq, effectTarget);

        Map<String, Object> payload = new HashMap<>();
        payload.put("spellName", result.getSpellName());
        if (result.getSlotLevelUsed() != null) {
            payload.put("slotLevel", result.getSlotLevelUsed());
        }
        if (result.getActionType() != null) {
            payload.put("actionType", result.getActionType());
        }
        payload.put("effectsApplied", result.getEffectsApplied());
        if (targetCombatantId != null) {
            payload.put("targetCombatantId", targetCombatantId.toString());
        }
        battleLogService.append(battleId, campaignId, BattleLogType.SPELL, caster.getId(), targetCombatantId,
                payload, BattleLogVisibility.PUBLIC, user.getId());

        // 2.1b/2.1c: resolve the plan's damage/healing onto combatants (incl. monsters) through the
        // shared save/mitigation/HP pipeline. AUTO rolls the dice; MANUAL uses the player's total.
        // AoE (2.3): with targetCombatantIds each covered combatant resolves individually (own save,
        // own resistance) against the same rolled/entered total. Reads SPELL → SAVE → DAMAGE in the log.
        List<UUID> aoeTargets = request.getTargetCombatantIds();
        if (aoeTargets != null && !aoeTargets.isEmpty()) {
            int total = 0;
            String modifier = null;
            for (UUID aoeTarget : aoeTargets) {
                boolean inBattle = combatants.stream().anyMatch(c -> c.getId().equals(aoeTarget));
                if (!inBattle) {
                    throw new BadRequestException("AoE target does not belong to this battle: " + aoeTarget);
                }
                SpellDamageSummary one = applySpellPlanOutcome(result.getPlan(), aoeTarget, caster, user,
                        campaignId, battleId, request.getDamageRollMode(), request.getManualDamage());
                total += one.total();
                if (one.modifier() != null) {
                    modifier = one.modifier();
                }
            }
            if (total > 0) {
                result.setAppliedDamage(total);
                result.setAppliedDamageModifier(modifier);
            }
        } else {
            SpellDamageSummary dmg = applySpellPlanOutcome(result.getPlan(), targetCombatantId, caster, user,
                    campaignId, battleId, request.getDamageRollMode(), request.getManualDamage());
            if (dmg.applied()) {
                result.setAppliedDamage(dmg.total());
                result.setAppliedDamageModifier(dmg.modifier());
            }
        }

        // 2.3: a lingering zone (Web) materializes on the battle's live map sessions — best-effort,
        // after commit semantics are relaxed (the cast never depends on map-service being up).
        FeatureExecutionPlan plan = result.getPlan();
        if (plan != null && plan.getZone() != null && plan.getZone().isPersists()
                && plan.getArea() != null && request.getOriginX() != null && request.getOriginY() != null) {
            mapZoneCreator.createZoneForBattle(battleId, new com.dnd.app.integration.map.MapZoneCreator.ZoneSpec(
                    plan.getArea().getShape(),
                    request.getOriginX(),
                    request.getOriginY(),
                    plan.getArea().getSizeFt() != null ? plan.getArea().getSizeFt() : 0,
                    request.getRotationDeg() != null ? request.getRotationDeg() : 0,
                    result.getSpellName(),
                    plan.getZone().getTerrain(),
                    plan.getZone().getObscurement(),
                    caster.getId()));
        }

        log.info("Spell cast in battle: battleId={}, caster={}, spell={}, by={}",
                battleId, caster.getDisplayName(), result.getSpellName(), username);
        return result;
    }

    /**
     * Выполняет активное умение персонажа в бою через общий feature-rules runtime.
     *
     * @param campaignId идентификатор кампании, в которой идет бой
     * @param battleId идентификатор активного боя
     * @param request параметры выбора умения, целей и режима броска урона
     * @param username пользователь, который инициировал действие
     * @return результат использования умения и актуальное состояние боя
     */
    @Transactional
    public BattleUseAbilityResult useAbility(UUID campaignId, UUID battleId,
            BattleUseAbilityRequest request, String username) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        User user = getUser(username);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Abilities can only be used in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant current = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        BattleCombatant actor = current;
        if (request.getCombatantId() != null) {
            actor = combatants.stream()
                    .filter(c -> request.getCombatantId().equals(c.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Ability actor does not belong to this battle"));
        }
        enforceControls(campaignId, user, actor);
        if (actor.getType() != CombatantType.CHARACTER || actor.getCharacter() == null) {
            throw new BadRequestException("Only characters can use abilities in battle");
        }
        if (!commandDedupService.firstSeen(request.getClientCommandId())) {
            Optional<BattleUseAbilityResult> replay = replayUseAbilityResult(request.getClientCommandId());
            if (replay.isPresent()) {
                return replay.get();
            }
            return BattleUseAbilityResult.builder()
                    .featureId(request.getFeatureId())
                    .outcome("DUPLICATE")
                    .battle(toResponse(battle, orderedCombatants(battleId)))
                    .message("Ability command already processed")
                    .build();
        }

        UUID primaryTargetId = request.getTargetCombatantId();
        List<UUID> targetIds = request.getTargetCombatantIds();
        if (targetIds != null && !targetIds.isEmpty()) {
            for (UUID targetId : targetIds) {
                ensureTargetInBattle(combatants, targetId, "Ability target does not belong to this battle: " + targetId);
            }
            primaryTargetId = targetIds.get(0);
        } else if (primaryTargetId != null) {
            ensureTargetInBattle(combatants, primaryTargetId, "Ability target does not belong to this battle");
            targetIds = List.of(primaryTargetId);
        } else {
            targetIds = List.of();
        }

        boolean itemAbility = request.getItemInstanceId() != null;
        if (!actor.getId().equals(current.getId())) {
            if (itemAbility) {
                throw new BadRequestException("Item abilities cannot be used off-turn yet");
            }
            AvailableFeatureAction action = findFeatureAction(actor, battleId, request.getFeatureId());
            if (action == null || !"reaction".equals(action.getActionType())) {
                throw new BadRequestException("Only reaction abilities can be used off-turn");
            }
            if (!action.isAvailable()) {
                throw new BadRequestException(action.getUnavailableReason() != null
                        ? action.getUnavailableReason()
                        : "Reaction ability is not available");
            }
        }
        FeatureExecutionPlan plan = itemAbility
                ? itemAbilityUseService.plan(actor.getCharacter(), request.getItemInstanceId(), request.getFeatureId())
                : combatFeatureExecutionService.plan(actor.getCharacter(), request.getFeatureId());
        if (plan.isRequiresManualAdjudication()) {
            Map<String, Object> manualPayload = new HashMap<>();
            manualPayload.put("featureId", request.getFeatureId().toString());
            manualPayload.put("outcome", "MANUAL_REQUIRED");
            if (itemAbility) {
                manualPayload.put("itemInstanceId", request.getItemInstanceId().toString());
            }
            battleLogService.append(battleId, campaignId, BattleLogType.FEATURE_USE, actor.getId(), primaryTargetId,
                    manualPayload,
                    BattleLogVisibility.GM_ONLY, user.getId());
            BattleUseAbilityResult result = BattleUseAbilityResult.builder()
                    .featureId(request.getFeatureId())
                    .featureName(plan.getFeatureName())
                    .outcome("MANUAL_REQUIRED")
                    .targetCombatantId(primaryTargetId)
                    .targetName(combatantName(combatants, primaryTargetId))
                    .plan(plan)
                    .battle(toResponse(battle, orderedCombatants(battleId)))
                    .message("Ability requires manual adjudication")
                    .build();
            storeUseAbilityReplay(request.getClientCommandId(), result);
            return result;
        }

        FeatureUseRequest useRequest = FeatureUseRequest.builder()
                .combatId(battleId)
                .targetIds(targetIds)
                .build();
        FeatureUseResult use = itemAbility
                ? itemAbilityUseService.use(actor.getCharacter(), request.getItemInstanceId(), request.getFeatureId(), useRequest)
                : featureUseService.use(actor.getCharacter(), request.getFeatureId(), useRequest);

        int total = 0;
        String modifier = null;
        if (!targetIds.isEmpty()) {
            for (UUID targetId : targetIds) {
                SpellDamageSummary one = applyPlanOutcome(plan, targetId, actor, user, campaignId, battleId,
                        request.getDamageRollMode(), request.getManualDamage(),
                        BattleLogType.FEATURE_USE, "FEATURE_DAMAGE_MANUAL");
                total += one.total();
                if (one.modifier() != null) {
                    modifier = one.modifier();
                }
            }
        } else {
            SpellDamageSummary one = applyPlanOutcome(plan, null, actor, user, campaignId, battleId,
                    request.getDamageRollMode(), request.getManualDamage(),
                    BattleLogType.FEATURE_USE, "FEATURE_DAMAGE_MANUAL");
            if (one.applied()) {
                total = one.total();
                modifier = one.modifier();
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("featureId", request.getFeatureId().toString());
        payload.put("featureName", use.getFeatureName());
        payload.put("outcome", "USED");
        if (itemAbility) {
            payload.put("itemInstanceId", request.getItemInstanceId().toString());
            // Контракт WS §4.5: явные source-поля для предметного источника умения.
            payload.put("sourceItemInstanceId", request.getItemInstanceId().toString());
            payload.put("sourceItemName", use.getFeatureName());
        }
        if (use.getActionType() != null) {
            payload.put("actionType", use.getActionType());
        }
        if (primaryTargetId != null) {
            payload.put("targetCombatantId", primaryTargetId.toString());
        }
        if (total > 0) {
            payload.put("appliedDamage", total);
        }
        battleLogService.append(battleId, campaignId, BattleLogType.FEATURE_USE, actor.getId(), primaryTargetId,
                payload, BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        BattleUseAbilityResult result = BattleUseAbilityResult.builder()
                .featureId(use.getFeatureId())
                .featureName(use.getFeatureName())
                .actionType(use.getActionType())
                .resourceKey(use.getResourceKey())
                .resourceSpent(use.getResourceSpent())
                .resourceRemaining(use.getResourceRemaining())
                .logId(use.getLogId())
                .outcome("USED")
                .targetCombatantId(primaryTargetId)
                .targetName(combatantName(combatants, primaryTargetId))
                .plan(plan)
                .appliedDamage(total > 0 ? total : null)
                .appliedDamageModifier(modifier)
                .battle(toResponse(battle, orderedCombatants(battleId)))
                .message(use.getMessage())
                .build();
        storeUseAbilityReplay(request.getClientCommandId(), result);
        return result;
    }

    private Optional<BattleUseAbilityResult> replayUseAbilityResult(UUID clientCommandId) {
        return commandDedupService.replayResponse(clientCommandId, USE_ABILITY_COMMAND)
                .flatMap(body -> {
                    try {
                        return Optional.of(objectMapper.readValue(body, BattleUseAbilityResult.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to read use-ability replay for command {}: {}",
                                clientCommandId, e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    private void storeUseAbilityReplay(UUID clientCommandId, BattleUseAbilityResult result) {
        if (clientCommandId == null || result == null) {
            return;
        }
        try {
            commandDedupService.storeResponse(clientCommandId, USE_ABILITY_COMMAND,
                    objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.warn("Failed to store use-ability replay for command {}: {}", clientCommandId, e.getMessage());
        }
    }

    /**
     * Возвращает preview плана классового умения для активного участника боя.
     *
     * @param campaignId идентификатор кампании
     * @param battleId идентификатор активного боя
     * @param featureId идентификатор классового умения
     * @param username пользователь, запрашивающий текущий ход
     * @return структурированный план исполнения или manual-required план для неоцифрованного умения
     */
    @Transactional(readOnly = true)
    public FeatureExecutionPlan planAbility(UUID campaignId, UUID battleId, UUID featureId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() != BattleStatus.ACTIVE) {
            throw new BadRequestException("Battle is not active");
        }
        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant current = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        if (current.getType() != CombatantType.CHARACTER || current.getCharacter() == null) {
            throw new BadRequestException("Only character abilities can be previewed");
        }
        return combatFeatureExecutionService.plan(current.getCharacter(), featureId);
    }

    private AvailableFeatureAction findFeatureAction(BattleCombatant actor, UUID battleId, UUID featureId) {
        if (featureActionService == null || actor == null || actor.getCharacter() == null) {
            return null;
        }
        return featureActionService.listAvailableActions(actor.getCharacter(), battleId).stream()
                .filter(action -> featureId.equals(action.getFeatureId()))
                .findFirst()
                .orElse(null);
    }

    /** Total damage dealt to the spell target + the resistance modifier applied (for the cast result). */
    private record SpellDamageSummary(int total, String modifier) {
        boolean applied() {
            return total > 0;
        }
        static SpellDamageSummary none() {
            return new SpellDamageSummary(0, null);
        }
    }

    private void ensureTargetInBattle(List<BattleCombatant> combatants, UUID targetId, String message) {
        boolean inBattle = combatants.stream().anyMatch(c -> c.getId().equals(targetId));
        if (!inBattle) {
            throw new BadRequestException(message);
        }
    }

    private String combatantName(List<BattleCombatant> combatants, UUID combatantId) {
        if (combatantId == null) {
            return null;
        }
        return combatants.stream()
                .filter(c -> c.getId().equals(combatantId))
                .map(BattleCombatant::getDisplayName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Apply a spell's execution plan to combatants (Phase 2.1b/2.1c). Damage lands only on an explicit
     * target (incl. monsters); healing lands on the target if given, else the caster (self-heal). The
     * spell's active effects were already applied by {@link SpellCastService#cast}; here we resolve
     * the numeric damage/healing the plan left for the caller, reusing the attack save/mitigation/HP
     * pipeline. {@code AUTO} rolls the dice; {@code MANUAL} uses {@code manualDamage} (the player's
     * physical roll) for the first damage line. Scope: single target; attack-roll spells (no attack
     * bonus in the plan) and AoE (multiple targets) are logged for GM adjudication, not mis-applied.
     */
    private SpellDamageSummary applySpellPlanOutcome(FeatureExecutionPlan plan, UUID targetCombatantId,
            BattleCombatant casterCombatant, User user, UUID campaignId, UUID battleId,
            String damageRollMode, Integer manualDamage) {
        return applyPlanOutcome(plan, targetCombatantId, casterCombatant, user, campaignId, battleId,
                damageRollMode, manualDamage, BattleLogType.SPELL, "SPELL_DAMAGE_MANUAL");
    }

    private SpellDamageSummary applyPlanOutcome(FeatureExecutionPlan plan, UUID targetCombatantId,
            BattleCombatant casterCombatant, User user, UUID campaignId, UUID battleId,
            String damageRollMode, Integer manualDamage, BattleLogType manualLogType, String manualEvent) {
        if (plan == null) {
            return SpellDamageSummary.none();
        }
        boolean manual = "MANUAL".equalsIgnoreCase(damageRollMode);
        BattleCombatant target = targetCombatantId == null ? null
                : combatantRepository.findByIdForUpdate(targetCombatantId).orElse(null);

        // Save ability for save-for-half comes from a saving_throw resolution: abilityId → StatType slug.
        String saveAbilityCode = plan.getResolutions() == null ? null : plan.getResolutions().stream()
                .filter(r -> "saving_throw".equalsIgnoreCase(r.getResolutionType()) && r.getAbilityId() != null)
                .findFirst()
                .flatMap(r -> statTypeRepository.findById(r.getAbilityId()))
                .map(StatType::getSlug)
                .orElse(null);

        int totalDamage = 0;
        String modifier = null;
        if (plan.getDamages() != null && target != null) {
            List<FeatureExecutionPlan.Damage> damages = plan.getDamages();
            for (int i = 0; i < damages.size(); i++) {
                // MANUAL applies the player's rolled total to the FIRST damage line; extra lines auto-roll.
                Integer manualForLine = manual && i == 0 ? manualDamage : null;
                SpellDamageSummary line = applySpellDamage(
                        damages.get(i), target, saveAbilityCode, manualForLine, user, campaignId, battleId,
                        manualLogType, manualEvent);
                totalDamage += line.total();
                if (line.modifier() != null && !"NONE".equals(line.modifier())) {
                    modifier = line.modifier();
                }
            }
        }

        if (plan.getHealings() != null) {
            BattleCombatant healTarget = target != null ? target
                    : combatantRepository.findByIdForUpdate(casterCombatant.getId()).orElse(casterCombatant);
            for (FeatureExecutionPlan.Healing heal : plan.getHealings()) {
                // Temp-HP healing needs its own accounting (not modelled here) → skip for now.
                if (heal.getAmount() != null && heal.getAmount() > 0 && !heal.isTempHp()) {
                    applyDamageOrHeal(healTarget, heal.getAmount(), user, campaignId);
                }
            }
        }
        return new SpellDamageSummary(totalDamage, modifier);
    }

    /**
     * Roll (AUTO) or take the player's total (MANUAL) for one plan damage line, apply save-for-half and
     * the target's resistance/immunity/vulnerability, then deal it. Returns the final damage + modifier.
     */
    private SpellDamageSummary applySpellDamage(FeatureExecutionPlan.Damage dmg, BattleCombatant target,
            String saveAbilityCode, Integer manualDamage, User user, UUID campaignId, UUID battleId,
            BattleLogType manualLogType, String manualEvent) {
        // Attack-roll spells need a spell-attack bonus the plan does not carry → GM adjudicates.
        if (dmg.isRequiresAttackHit()) {
            logSpellManual(battleId, campaignId, target, "attack_roll", user, manualLogType, manualEvent);
            return SpellDamageSummary.none();
        }
        int rolled;
        if (manualDamage != null) {
            // The player rolled the dice physically and entered the total (pre-save, pre-resistance).
            rolled = Math.max(0, manualDamage);
        } else {
            rolled = 0;
            if (dmg.getDiceExpression() != null && !dmg.getDiceExpression().isBlank()) {
                rolled += diceRoller.rollDamage(dmg.getDiceExpression(), false);
            }
            if (dmg.getFlatAmount() != null) {
                rolled += dmg.getFlatAmount();
            }
            rolled = Math.max(0, rolled);
        }

        if (dmg.isRequiresSave() && dmg.getSaveDc() != null) {
            if (saveAbilityCode == null) {
                logSpellManual(battleId, campaignId, target, "unresolved_save", user, manualLogType, manualEvent);
                return SpellDamageSummary.none();
            }
            int d20 = diceRoller.rollD20();
            int saveBonus = resolveTargetSaveBonus(target, saveAbilityCode);
            AttackResolver.SaveOutcome save = AttackResolver.resolveSave(d20, saveBonus, dmg.getSaveDc());
            if (save == AttackResolver.SaveOutcome.SUCCESS) {
                rolled = dmg.isHalfOnSave() ? rolled / 2 : 0;
            }
            Map<String, Object> saveLog = new HashMap<>();
            saveLog.put("targetName", target.getDisplayName());
            saveLog.put("outcome", save.name());
            saveLog.put("saveDc", dmg.getSaveDc());
            saveLog.put("saveBonus", saveBonus);
            saveLog.put("saveTotal", d20 + saveBonus);
            battleLogService.append(battleId, campaignId, BattleLogType.SAVE, null, target.getId(),
                    saveLog, BattleLogVisibility.PUBLIC, user.getId());
        }

        if (rolled <= 0) {
            return SpellDamageSummary.none();
        }
        DamageMitigationService.Mitigation mit = damageMitigationService.mitigate(target, rolled, dmg.getDamageTypeId());
        int finalDamage = mit.finalDamage();
        if (finalDamage > 0) {
            applyDamageOrHeal(target, -finalDamage, user, campaignId);
        }
        return new SpellDamageSummary(finalDamage, mit.modifier().name());
    }

    private void logSpellManual(UUID battleId, UUID campaignId, BattleCombatant target, String reason, User user,
            BattleLogType manualLogType, String manualEvent) {
        battleLogService.append(battleId, campaignId, manualLogType, null, target.getId(),
                Map.of("event", manualEvent, "reason", reason), BattleLogVisibility.GM_ONLY, user.getId());
    }

    /**
     * Выполняет операции "group initiative" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantIds входящее значение combatant ids, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse groupInitiative(UUID campaignId, UUID battleId, List<UUID> combatantIds, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Initiative can only be rolled in an active battle");
        if (combatantIds == null || combatantIds.isEmpty()) {
            throw new BadRequestException("No combatants selected");
        }

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, combatants);
        java.util.Set<UUID> ids = new java.util.HashSet<>(combatantIds);
        int d20 = diceRoller.rollD20();
        int applied = 0;
        for (BattleCombatant c : combatants) {
            if (!ids.contains(c.getId())) {
                continue;
            }
            int initiative;
            if (c.getType() == CombatantType.MONSTER && c.getMonster() != null) {
                int dex = c.getMonster().getDexScore() != null ? c.getMonster().getDexScore().intValue() : 10;
                Integer bonus = c.getMonster().getInitiativeBonus() != null
                        ? c.getMonster().getInitiativeBonus().intValue() : null;
                initiative = CombatCalculator.monsterInitiative(d20, bonus, dex);
            } else if (c.getCharacter() != null) {
                PlayerCharacter character = c.getCharacter();
                initiative = CombatCalculator.characterInitiative(d20, dexValue(character), dexBuffBonus(character));
            } else {
                initiative = d20;
            }
            c.setInitiativeRoll(d20);
            c.setInitiative(initiative);
            applied++;
        }
        CombatCalculator.orderTracker(combatants);
        combatantRepository.saveAll(combatants);
        battle.setCurrentTurnIndex(
                CombatCalculator.resolveCurrentIndex(combatants, activeId, battle.getCurrentTurnIndex()));
        battleRepository.save(battle);

        battleLogService.append(battleId, campaignId, BattleLogType.TURN, null, null,
                Map.of("event", "GROUP_INITIATIVE", "d20", d20, "count", applied),
                BattleLogVisibility.PUBLIC, user.getId());
        log.info("Group initiative rolled: battleId={}, d20={}, count={}, by={}", battleId, d20, applied, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    private PlayerCharacter requireDyingCharacter(BattleCombatant combatant) {
        if (combatant.getType() != CombatantType.CHARACTER || combatant.getCharacter() == null) {
            throw new BadRequestException("Only characters make death saving throws");
        }
        PlayerCharacter character = combatant.getCharacter();
        if (nz(combatant.getCurrentHp()) > 0 || character.getStatus() == CharacterStatus.DEAD) {
            throw new BadRequestException("Death saves apply only to a dying character (0 HP, not dead)");
        }
        return character;
    }

    private void addDeathSaveFailures(PlayerCharacter character, int amount) {
        int failures = Math.min(3, nz(character.getDeathSaveFailures()) + amount);
        character.setDeathSaveFailures(failures);
        if (failures >= 3) {
            character.setStatus(CharacterStatus.DEAD);
        }
    }

    // ============================== Group assembly ==============================

    /**
     * Добавляет результат операции "add monsters" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse addMonsters(UUID campaignId, UUID battleId,
                                      AddBattleMonstersRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Monsters can only be added while assembling the group");

        for (AddBattleMonstersRequest.MonsterEntry entry : request.getMonsters()) {
            Monster monster = monsterService.getUsableCampaignMonster(campaignId, entry.getMonsterId(), username);
            int existing = (int) combatantRepository
                    .countByBattleIdAndTypeAndMonsterId(battleId, CombatantType.MONSTER, monster.getId());
            for (int i = 1; i <= entry.getCount(); i++) {
                int index = existing + i;
                BattleCombatant combatant = BattleCombatant.builder()
                        .battle(battle)
                        .type(CombatantType.MONSTER)
                        .monster(monster)
                        .displayName(monster.getNameRusloc() + " #" + index)
                        .instanceIndex(index)
                        .dexTiebreak(monster.getDexScore() != null ? monster.getDexScore().intValue() : null)
                        .currentHp(monster.getHpAverage())
                        .maxHp(monster.getHpAverage())
                        // Monster runtime (Phase 2.9): legendary actions from the statblock and the
                        // Legendary Resistance pool parsed from its trait text.
                        .legendaryActionMax(monster.getLegendaryUsesBase() != null
                                ? monster.getLegendaryUsesBase().intValue() : 0)
                        .legendaryResistanceMax(parseLegendaryResistance(monster))
                        .addedBy(user)
                        .build();
                combatantRepository.save(combatant);
            }
        }

        log.info("Monsters added to battle: battleId={}, entries={}, by={}",
                battleId, request.getMonsters().size(), username);
        List<BattleCombatant> combatants = orderedCombatants(battleId);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    /**
     * Удаляет результат операции "remove combatant" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse removeCombatant(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Combatants can only be removed while assembling the group");

        BattleCombatant combatant = combatantRepository.findById(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new ResourceNotFoundException("Combatant does not belong to this battle");
        }
        combatantRepository.delete(combatant);

        log.info("Combatant removed from battle: battleId={}, combatantId={}, by={}", battleId, combatantId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Устанавливает результат операции "set override xp" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse setOverrideXp(UUID campaignId, UUID battleId,
                                        UpdateBattleXpRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() == BattleStatus.COMPLETED) {
            throw new BadRequestException("Cannot change XP of a completed battle");
        }

        battle.setOverrideXp(request.getOverrideXp());
        battleRepository.save(battle);

        log.info("Battle XP override set: battleId={}, overrideXp={}, by={}",
                battleId, request.getOverrideXp(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    // ================================== Start ==================================

    /**
     * Выполняет операции "start battle" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse startBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        // Lock the battle row so a second concurrent start can't also roll initiative / reorder.
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Battle has already started");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Cannot start a battle with no combatants");
        }

        for (BattleCombatant c : combatants) {
            if (c.getType() == CombatantType.MONSTER) {
                int roll = diceRoller.rollD20();
                int dex = c.getMonster().getDexScore() != null ? c.getMonster().getDexScore().intValue() : 10;
                Integer bonus = c.getMonster().getInitiativeBonus() != null
                        ? c.getMonster().getInitiativeBonus().intValue() : null;
                c.setInitiativeRoll(roll);
                c.setInitiative(CombatCalculator.monsterInitiative(roll, bonus, dex));
            }
        }
        CombatCalculator.orderTracker(combatants);
        combatantRepository.saveAll(combatants);

        battle.setStatus(BattleStatus.ACTIVE);
        battle.setRoundNumber(1);
        battle.setCurrentTurnIndex(0);
        battle.setStartedAt(Instant.now());
        battleRepository.save(battle);

        log.info("Battle started: id={}, combatants={}, by={}", battleId, combatants.size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_STARTED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    // =============================== Player join ===============================

    /**
     * Выполняет операции "join characters" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse joinCharacters(UUID campaignId, UUID battleId,
                                         JoinBattleRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        // Lock the battle row so concurrent joins serialize their tracker reorder + index re-anchor.
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Characters can only join an active battle");

        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());

        // Remember whose turn it is so the order stays anchored after we re-sort
        List<BattleCombatant> before = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, before);

        for (JoinBattleRequest.CharacterJoin join : request.getCharacters()) {
            PlayerCharacter character = characterRepository.findById(join.getCharacterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

            if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
                throw new BadRequestException("Character does not belong to this campaign");
            }
            if (!gm && !character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You can only add your own characters to the battle");
            }
            if (combatantRepository.existsByBattleIdAndCharacterId(battleId, character.getId())) {
                throw new BadRequestException("Character '" + character.getName() + "' is already in this battle");
            }

            int d20 = (join.getD20() != null) ? join.getD20() : diceRoller.rollD20();
            int dexValue = dexValue(character);
            int dexBuff = dexBuffBonus(character);
            int initiative = CombatCalculator.characterInitiative(d20, dexValue, dexBuff);

            BattleCombatant combatant = BattleCombatant.builder()
                    .battle(battle)
                    .type(CombatantType.CHARACTER)
                    .character(character)
                    .displayName(character.getName())
                    .initiativeRoll(d20)
                    .initiative(initiative)
                    .dexTiebreak(dexValue)
                    .currentHp(character.getCurrentHp())
                    .maxHp(character.getMaxHp())
                    .addedBy(user)
                    .build();
            combatantRepository.save(combatant);
        }

        // Re-order the whole tracker and keep the turn on the same combatant
        List<BattleCombatant> after = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        CombatCalculator.orderTracker(after);
        combatantRepository.saveAll(after);
        battle.setCurrentTurnIndex(
                CombatCalculator.resolveCurrentIndex(after, activeId, battle.getCurrentTurnIndex()));
        battleRepository.save(battle);

        log.info("Characters joined battle: battleId={}, count={}, by={}",
                battleId, request.getCharacters().size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.COMBATANT_JOINED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, after);
    }

    /**
     * Возвращает результат операции "get character initiative bonus" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public int getCharacterInitiativeBonus(UUID campaignId, UUID battleId, UUID characterId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        findBattle(battleId, campaignId);

        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        if (!gm && !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only preview your own characters");
        }
        return CombatCalculator.abilityModifier(dexValue(character)) + dexBuffBonus(character);
    }

    // ============================== Turn passing ===============================

    /**
     * Выполняет операции "end turn" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    /**
     * Передаёт ход следующему комбатанту (совместимость): без защиты от повторов. Делегирует
     * mode-aware версии с {@code null}-параметрами надёжности.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param username   инициатор (владелец активного персонажа или GM)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse endTurn(UUID campaignId, UUID battleId, String username) {
        return endTurn(campaignId, battleId, null, null, null, username);
    }

    /**
     * Передаёт ход следующему комбатанту с защитой realtime (фаза 2.14): {@code clientCommandId}
     * даёт идемпотентность (повторная отправка не сдвигает ход второй раз), а {@code expectedTurnIndex}
     * /{@code expectedRound} защищают от двойного next-turn — если клиентский взгляд на ход устарел
     * (ход уже сменился), запрос отклоняется. Тик состояний/эффектов на границе раунда сохранён.
     *
     * @param campaignId        идентификатор кампании
     * @param battleId          идентификатор боя
     * @param expectedTurnIndex ожидаемый клиентом текущий индекс хода ({@code null} — проверка пропускается)
     * @param expectedRound     ожидаемый клиентом номер раунда ({@code null} — проверка пропускается)
     * @param clientCommandId   идемпотентный ключ команды ({@code null} — без дедупа)
     * @param username          инициатор (владелец активного персонажа или GM)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse endTurn(UUID campaignId, UUID battleId, Integer expectedTurnIndex,
                                  Integer expectedRound, UUID clientCommandId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        // Row-lock the battle so two simultaneous end-turn calls can't both advance it.
        Battle battle = battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
        requireStatus(battle, BattleStatus.ACTIVE, "Turns can only be passed in an active battle");

        // Идемпотентность: повторная команда с тем же ключом не сдвигает ход второй раз (фаза 2.14).
        if (!commandDedupService.firstSeen(clientCommandId)) {
            return toResponse(battle, orderedCombatants(battleId));
        }
        // Защита от двойного next-turn: устаревший клиентский индекс/раунд → ход уже сменился.
        if (expectedTurnIndex != null && battle.getCurrentTurnIndex() != expectedTurnIndex.intValue()) {
            throw new BadRequestException("The turn has already advanced");
        }
        if (expectedRound != null && battle.getRoundNumber() != expectedRound.intValue()) {
            throw new BadRequestException("The round has already advanced");
        }

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }

        int currentIndex = clampIndex(battle.getCurrentTurnIndex(), combatants.size());
        BattleCombatant current = combatants.get(currentIndex);
        enforceCanEndTurn(campaignId, user, current);

        // Внезапность (3.7) заканчивается по окончании первого хода застигнутого существа.
        if (Boolean.TRUE.equals(current.getSurprised())) {
            current.setSurprised(false);
            combatantRepository.save(current);
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex >= combatants.size()) {
            nextIndex = 0;
            battle.setRoundNumber(battle.getRoundNumber() + 1);
            // New round: tick down timed effects on every joined character — BOTH systems, from the one
            // place a round advances. Legacy buffs (decrementRounds) and feature effects (tickRounds)
            // previously only the former was wired here; the latter ticked solely from its controller.
            for (BattleCombatant c : combatants) {
                if (c.getType() == CombatantType.CHARACTER && c.getCharacter() != null) {
                    characterEffectService.decrementRounds(c.getCharacter().getId());
                    effectExpirationService.tickRounds(c.getCharacter().getId());
                }
            }
            // Conditions with a finite duration tick down on the round boundary too (Phase 1.1).
            conditionService.tick(battleId);
            battleLogService.append(battleId, campaignId, BattleLogType.ROUND, null, null,
                    Map.of("round", battle.getRoundNumber()), BattleLogVisibility.PUBLIC, user.getId());
        }
        battle.setCurrentTurnIndex(nextIndex);
        battleRepository.save(battle);

        // The combatant now on turn starts with a fresh action economy.
        BattleCombatant nowOnTurn = combatants.get(nextIndex);
        resetActionEconomy(nowOnTurn);

        // Recharge abilities roll to recharge at the start of the monster's turn (Phase 2.9).
        List<String> recharged = rollRecharge(nowOnTurn);
        if (!recharged.isEmpty()) {
            combatantRepository.save(nowOnTurn);
            battleLogService.append(battleId, campaignId, BattleLogType.EFFECT, nowOnTurn.getId(), null,
                    Map.of("event", "RECHARGE", "combatant", nowOnTurn.getDisplayName(), "abilities", recharged),
                    BattleLogVisibility.PUBLIC, user.getId());
        }

        battleLogService.append(battleId, campaignId, BattleLogType.TURN, nowOnTurn.getId(), null,
                Map.of("combatantName", nowOnTurn.getDisplayName(), "turnIndex", nextIndex,
                        "round", battle.getRoundNumber()), BattleLogVisibility.PUBLIC, user.getId());

        log.info("Turn passed: battleId={}, newIndex={}, round={}, by={}",
                battleId, nextIndex, battle.getRoundNumber(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                java.util.Map.of("battleId", battleId, "currentTurnIndex", nextIndex,
                        "currentCombatantId", nowOnTurn.getId()), user.getId());
        return toResponse(battle, combatants);
    }

    /**
     * Возвращает результат операции "get current turn" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CombatantTurnResponse getCurrentTurn(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() != BattleStatus.ACTIVE) {
            throw new BadRequestException("Battle is not active");
        }

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant current = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        return buildCombatantTurn(current, battle, user, campaignId, username);
    }

    /**
     * The full actionable detail (attacks / abilities / resources) for ANY combatant in an active
     * battle — used both for the current turn and, off-turn, to resolve a reaction / opportunity
     * attack by a non-active combatant (Phase 2.8). GM-only for a monster's statblock.
     */
    @Transactional(readOnly = true)
    public CombatantTurnResponse getCombatantTurn(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() != BattleStatus.ACTIVE) {
            throw new BadRequestException("Battle is not active");
        }
        BattleCombatant combatant = combatantRepository.findById(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        return buildCombatantTurn(combatant, battle, user, campaignId, username);
    }

    /** Builds the actionable turn detail for one combatant (shared by current-turn and reaction flows). */
    private CombatantTurnResponse buildCombatantTurn(BattleCombatant current, Battle battle, User user,
                                                     UUID campaignId, String username) {
        CombatantTurnResponse.CombatantTurnResponseBuilder builder = CombatantTurnResponse.builder()
                .combatant(toCombatantResponse(current, battle));

        if (current.getType() == CombatantType.CHARACTER && current.getCharacter() != null) {
            UUID characterId = current.getCharacter().getId();
            CharacterResponse characterResponse = characterService.getCharacterById(characterId, username);
            // Surface weapon-driven attacks (equipped weapons + proficiency, incl. throw variants)
            // and progression-based class attacks alongside any manually-authored attacks so the
            // combat UI lists every actionable strike/throw/class action.
            List<CharacterAttackResponse> derived = new ArrayList<>(weaponAttackService.computeAttacks(current.getCharacter()));
            derived.addAll(classAbilityCombatService.classAttacks(current.getCharacter()));
            if (!derived.isEmpty()) {
                if (characterResponse.getAttacks() != null) {
                    derived.addAll(characterResponse.getAttacks());
                }
                characterResponse.setAttacks(derived);
            }
            SpellSlotsResponse slots = spellSlotService.getSlots(characterId, username);
            boolean hasSlots = slots != null && slots.getLevels() != null && !slots.getLevels().isEmpty();
            builder.character(characterResponse)
                    .resources(characterResourceService.getResources(characterId, username))
                    .activeEffects(characterEffectService.getActiveEffects(characterId, username))
                    .classAbilities(classAbilityCombatService.listAbilities(current.getCharacter()))
                    .featureActions(featureActionService != null
                            ? featureActionService.listAvailableActions(current.getCharacter(), battle.getId())
                            : List.of())
                    .spellSlots(hasSlots ? slots : null)
                    .tacticalActions(buildCharacterTacticalActions(characterResponse));
        } else if (current.getType() == CombatantType.MONSTER && current.getMonster() != null) {
            boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
            if (gm) {
                MonsterResponse monster = monsterService.getMonster(current.getMonster().getId(), username);
                builder.monster(monster)
                        .tacticalActions(buildMonsterTacticalActions(monster));
            }
        }
        return builder.build();
    }

    // ===================== Tactical targeting metadata (read-only) =====================
    // Exposes range/AoE hints for the tactical map. Spatial state stays in map-service; nothing
    // here is parsed from free localized text — unreliable data is reported as UNKNOWN.

    private List<TacticalActionResponse> buildCharacterTacticalActions(CharacterResponse character) {
        List<TacticalActionResponse> actions = new ArrayList<>();
        if (character.getAttacks() != null) {
            for (CharacterAttackResponse atk : character.getAttacks()) {
                List<TacticalActionResponse.Damage> dmg = null;
                if (atk.getDamage() != null) {
                    dmg = List.of(TacticalActionResponse.Damage.builder()
                            .dice(atk.getDamage()).damageType(atk.getDamageType()).build());
                }
                actions.add(TacticalActionResponse.builder()
                        .id(atk.getName())
                        .name(atk.getName())
                        .source(mapAttackSource(atk.getSource()))
                        .actionCost("UNKNOWN")
                        .targeting(TacticalActionResponse.Targeting.builder()
                                // The localized range string is not reliably structured, so rangeFt
                                // stays null and the shape is UNKNOWN; an attack is always single-target.
                                .mode("SINGLE_TARGET")
                                .areaShape("UNKNOWN")
                                .requiresAttackRoll(true)
                                .requiresSavingThrow(false)
                                .build())
                        .damage(dmg)
                        .build());
            }
        }
        if (character.getKnownSpells() != null && !character.getKnownSpells().isEmpty()) {
            List<UUID> spellIds = character.getKnownSpells().stream()
                    .map(CharacterKnownSpellResponse::getSpellId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            for (Spell spell : spellRepository.findAllById(spellIds)) {
                actions.add(TacticalActionResponse.builder()
                        .id(spell.getId() != null ? spell.getId().toString() : spell.getSlug())
                        .name(spell.getNameRu())
                        .source("SPELL")
                        .actionCost(mapSpellActionCost(spell.getCastingActionSlug()))
                        .targeting(TacticalActionResponse.Targeting.builder()
                                .mode(isSelfRange(spell.getRangeType()) ? "SELF" : "UNKNOWN")
                                .rangeFt(structuredRangeFt(spell.getRangeDistance(), spell.getRangeUnit()))
                                // No structured area/radius/shape columns exist for spells — never
                                // invent them from text.
                                .areaShape("UNKNOWN")
                                .requiresAttackRoll(false)
                                .requiresSavingThrow(false)
                                .build())
                        .build());
            }
        }
        return actions;
    }

    private List<TacticalActionResponse> buildMonsterTacticalActions(MonsterResponse monster) {
        List<TacticalActionResponse> actions = new ArrayList<>();
        if (monster.getFeatures() == null) {
            return actions;
        }
        for (MonsterResponse.FeatureView f : monster.getFeatures()) {
            boolean hasAttack = f.getAttackType() != null;
            boolean hasSave = f.getSaveDc() != null;
            if (!hasAttack && !hasSave) {
                continue;
            }
            boolean requiresAttackRoll = hasAttack && f.getAttackBonus() != null;
            Short rangeShort = f.getRangeFt() != null ? f.getRangeFt() : f.getReachFt();
            Integer rangeFt = rangeShort != null ? rangeShort.intValue() : null;
            List<TacticalActionResponse.Damage> dmg = null;
            if (f.getDamages() != null && !f.getDamages().isEmpty()) {
                dmg = f.getDamages().stream()
                        .map(d -> TacticalActionResponse.Damage.builder()
                                .dice(d.getDice())
                                .damageType(d.getDamageType() != null ? d.getDamageType().getNameRusloc() : null)
                                .build())
                        .toList();
            }
            actions.add(TacticalActionResponse.builder()
                    .id(f.getId() != null ? f.getId().toString() : f.getNameRusloc())
                    .name(f.getNameRusloc())
                    .source("MONSTER_FEATURE")
                    .actionCost(mapFeatureActionCost(f.getKind()))
                    .targeting(TacticalActionResponse.Targeting.builder()
                            .mode(requiresAttackRoll ? "SINGLE_TARGET" : "UNKNOWN")
                            .rangeFt(rangeFt)
                            .areaShape("UNKNOWN")
                            .requiresAttackRoll(requiresAttackRoll)
                            .requiresSavingThrow(hasSave)
                            .build())
                    .damage(dmg)
                    .build());
        }
        return actions;
    }

    private String mapAttackSource(String source) {
        if (source == null) {
            return "MANUAL";
        }
        return switch (source.toUpperCase()) {
            case "WEAPON" -> "WEAPON";
            case "CLASS" -> "CLASS_ABILITY";
            default -> "MANUAL";
        };
    }

    private String mapSpellActionCost(String slug) {
        if (slug == null) {
            return "UNKNOWN";
        }
        String s = slug.toLowerCase();
        if (s.contains("bonus")) {
            return "BONUS_ACTION";
        }
        if (s.contains("reaction")) {
            return "REACTION";
        }
        if (s.contains("action")) {
            return "ACTION";
        }
        return "UNKNOWN";
    }

    private String mapFeatureActionCost(String kind) {
        if (kind == null) {
            return "UNKNOWN";
        }
        String k = kind.toLowerCase();
        if (k.contains("bonus")) {
            return "BONUS_ACTION";
        }
        if (k.contains("reaction")) {
            return "REACTION";
        }
        if (k.contains("action")) {
            return "ACTION";
        }
        return "UNKNOWN";
    }

    private boolean isSelfRange(String rangeType) {
        return rangeType != null && rangeType.equalsIgnoreCase("self");
    }

    /** Only surface a range in feet when the unit is reliably feet; otherwise leave it unknown (null). */
    private Integer structuredRangeFt(Integer distance, String unit) {
        if (distance == null || unit == null) {
            return null;
        }
        String u = unit.toLowerCase();
        boolean feet = u.contains("ft") || u.contains("feet") || u.contains("фут");
        return feet ? distance : null;
    }

    // ============================ Attacks & damage ============================

    /**
     * Выполняет операции "perform attack" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleActionResultResponse performAttack(UUID campaignId, UUID battleId,
                                                    BattleAttackRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Attacks can only happen in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        // An opportunity attack (or any reaction strike, Phase 2.8) is made out of turn by a named
        // combatant and spends its reaction; a normal attack is made by the active combatant and
        // spends its action. Both go through this one server-authoritative path (A3 — no side door).
        boolean reaction = Boolean.TRUE.equals(request.getReaction());
        boolean multiattack = false;
        BattleCombatant attacker;
        if (reaction) {
            if (request.getAttackerCombatantId() == null) {
                throw new BadRequestException("A reaction attack requires the attacker combatant id");
            }
            attacker = combatantRepository.findByIdForUpdate(request.getAttackerCombatantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attacker combatant not found"));
            if (!attacker.getBattle().getId().equals(battleId)) {
                throw new BadRequestException("Attacker does not belong to this battle");
            }
            enforceControls(campaignId, user, attacker);
            if (Boolean.TRUE.equals(attacker.getReactionUsed())) {
                throw new BadRequestException("The reaction has already been used this turn");
            }
        } else {
            attacker = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
            enforceControls(campaignId, user, attacker);
            attacker = combatantRepository.findByIdForUpdate(attacker.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Attacker combatant not found"));
            // A Multiattack monster spends its Attack action on several strikes (Phase 2.9): the
            // per-turn attacksRemaining budget guards it. Everyone else is one attack per action.
            multiattack = attacker.getType() == CombatantType.MONSTER && attacker.getAttacksRemaining() != null;
            if (multiattack) {
                if (attacker.getAttacksRemaining() <= 0) {
                    throw new BadRequestException("No attacks remain this turn");
                }
            } else if (attacker.getActionSpent() >= attacker.getActionMax()) {
                throw new BadRequestException("The action has already been used this turn");
            }
        }

        // Lock the target row before its HP changes (matters for monster targets, whose HP lives
        // only on the combatant row; character targets are additionally locked on the sheet).
        BattleCombatant target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }
        if (target.getId().equals(attacker.getId())) {
            throw new BadRequestException("A combatant cannot attack itself");
        }

        AttackOption attack = resolveAttack(attacker, request.getAttackName());

        // Range/reach gate (Phase 2.5): reject a strike out of reach or a shot beyond long range;
        // long range / ranged-in-melee force disadvantage below. GM may override the whole gate.
        // Recharge gate (Phase 2.9): a recharge ability (e.g. a breath weapon) can't be used again
        // until it recharges at the start of the monster's turn.
        if (attack.rechargeMin() != null && attack.featureId() != null
                && rechargeSpentIds(attacker).contains(attack.featureId())) {
            throw new BadRequestException("'" + attack.name() + "' is recharging and cannot be used yet");
        }

        RangeEvaluation range = evaluateRange(request, attack);
        boolean gmOverrideRange = Boolean.TRUE.equals(request.getGmOverrideRange());
        if (range.outOfRange() && !gmOverrideRange) {
            throw new BadRequestException("Target is out of range for '" + attack.name() + "' ("
                    + range.distanceFt() + " ft; " + rangeLabel(attack) + ")");
        }

        // Cover (Phase 2.6): manual selection. TOTAL cover cannot be targeted; HALF/THREE_QUARTERS
        // raise the target's AC (attack roll) and its Dexterity saving throw (save-based attacks).
        CoverType cover = request.getCover() != null ? request.getCover() : CoverType.NONE;
        if (cover == CoverType.TOTAL) {
            throw new BadRequestException("Target has total cover and cannot be targeted directly");
        }
        int coverBonus = cover.bonus();
        boolean coverAppliesToSave = coverBonus > 0
                && attack.saveAbilityCode() != null
                && "DEXTERITY".equalsIgnoreCase(attack.saveAbilityCode());

        // Feature-effect bonuses for a character attacker (to-hit and damage); 0 for monsters and when
        // no active effect contributes — additive, so existing behaviour is unchanged without them.
        int attackRollBonus = 0;
        int damageBonus = 0;
        if (attacker.getType() == CombatantType.CHARACTER && attacker.getCharacter() != null) {
            UUID attackerCharId = attacker.getCharacter().getId();
            attackRollBonus = modifierAggregator.totalFor(attackerCharId, ModifierTarget.attackRoll());
            damageBonus = modifierAggregator.totalFor(attackerCharId, ModifierTarget.damageDealt());
        }

        Integer damage = null;
        Integer targetAc = null;
        Integer attackBonusOut = null;
        Integer total = null;
        String outcomeName;
        // Saving-throw detail, populated only for save-based attacks.
        String saveAbilityOut = null;
        Integer saveBonusOut = null;
        Integer saveTotalOut = null;
        String saveRollModeOut = null;
        String damageModifierOut = null;

        RollResolution roll;
        AttackRollMode rollMode;
        int effectiveD20;

        // Standard-action modifiers (Phase 2.7). Attacking from hiding or with an ally's Help grants
        // advantage; a dodging target imposes disadvantage and gets advantage on its own Dex saves.
        // These combine with the range penalty and cancel per 5e (any advantage + any disadvantage = normal).
        boolean attackerAdvantage = Boolean.TRUE.equals(attacker.getHidden())
                || Boolean.TRUE.equals(attacker.getHelpAdvantage());
        boolean targetDodging = Boolean.TRUE.equals(target.getDodging());

        if (attack.saveDc() != null) {
            // Save-based attack (e.g. a monster's breath weapon): the TARGET rolls a saving throw with
            // its own bonus (ability modifier + proficiency/statblock save + active effects) against the
            // DC — the attacker makes no attack roll. Success halves the damage, failure takes it full.
            AttackRollMode requestedSaveMode = request.getSaveRollMode() != null
                    ? request.getSaveRollMode() : AttackRollMode.NORMAL;
            boolean saveAdvFromDodge = targetDodging
                    && "DEXTERITY".equalsIgnoreCase(attack.saveAbilityCode());
            if (saveAdvFromDodge && requestedSaveMode != AttackRollMode.ADVANTAGE) {
                // Dodging grants advantage on Dexterity saves — the server rolls it (Phase 2.7).
                rollMode = AttackRollMode.ADVANTAGE;
                roll = resolveRoll("save", AttackRollMode.ADVANTAGE, null, null, null);
            } else {
                rollMode = requestedSaveMode;
                roll = resolveRoll("save", rollMode, request.getSaveD20(), request.getSaveD20A(), request.getSaveD20B());
            }
            effectiveD20 = roll.effectiveD20();
            int saveBonus = resolveTargetSaveBonus(target, attack.saveAbilityCode())
                    + (coverAppliesToSave ? coverBonus : 0);
            AttackResolver.SaveOutcome save = AttackResolver.resolveSave(effectiveD20, saveBonus, attack.saveDc());
            int rolled = Math.max(0, diceRoller.rollDamage(attack.damage(), false) + damageBonus);
            damage = save == AttackResolver.SaveOutcome.SUCCESS ? rolled / 2 : rolled;
            DamageMitigationService.Mitigation saveMit = damageMitigationService.mitigate(target, damage, attack.damageTypeId());
            damage = saveMit.finalDamage();
            damageModifierOut = saveMit.modifier().name();
            outcomeName = save.name();
            total = effectiveD20 + saveBonus;
            saveAbilityOut = saveAbilityDisplayName(attack.saveAbilityCode());
            saveBonusOut = saveBonus;
            saveTotalOut = total;
            saveRollModeOut = rollMode.name();
        } else {
            AttackRollMode requestedMode = request.getRollMode() != null ? request.getRollMode() : AttackRollMode.NORMAL;
            // Combine every automatic source: advantage (hidden/help) vs disadvantage (range/dodge).
            // Per 5e any advantage and any disadvantage cancel to normal, regardless of count.
            boolean autoDisadvantage = (range.forcedDisadvantage() && !gmOverrideRange) || targetDodging;
            AttackRollMode autoMode = attackerAdvantage == autoDisadvantage ? null
                    : (attackerAdvantage ? AttackRollMode.ADVANTAGE : AttackRollMode.DISADVANTAGE);
            if (autoMode != null) {
                // The server enforces the automatic mode. It keeps the client's dice only if they
                // already match the enforced mode (its FE knew the state); otherwise it rolls virtually,
                // since a lone/opposite die is not trusted to reflect the enforced advantage/disadvantage.
                rollMode = autoMode;
                boolean clientPair = requestedMode == autoMode
                        && request.getD20A() != null && request.getD20B() != null;
                roll = clientPair
                        ? resolveRoll("attack", autoMode, null, request.getD20A(), request.getD20B())
                        : resolveRoll("attack", autoMode, null, null, null);
            } else {
                rollMode = requestedMode;
                roll = resolveAttackRoll(request);
            }
            effectiveD20 = roll.effectiveD20();
            targetAc = resolveTargetAc(target) + coverBonus;
            int effectiveAttackBonus = attack.attackBonus() + attackRollBonus;
            AttackResolver.Outcome outcome = AttackResolver.resolve(effectiveD20, effectiveAttackBonus, targetAc);
            if (outcome.dealsDamage()) {
                damage = Math.max(0, diceRoller.rollDamage(attack.damage(),
                        outcome == AttackResolver.Outcome.CRIT) + damageBonus);
                DamageMitigationService.Mitigation mit = damageMitigationService.mitigate(target, damage, attack.damageTypeId());
                damage = mit.finalDamage();
                damageModifierOut = mit.modifier().name();
            }
            outcomeName = outcome.name();
            attackBonusOut = effectiveAttackBonus;
            total = effectiveD20 + effectiveAttackBonus;
        }

        // Combat log: record the ATTACK (roll formula + dice + modifier) BEFORE applying damage, so the
        // log reads ATTACK → DAMAGE (→ DEATH_SAVE). The DAMAGE row is written by the HP primitive.
        Map<String, Object> attackLog = new HashMap<>();
        attackLog.put("attackerName", attacker.getDisplayName());
        attackLog.put("targetName", target.getDisplayName());
        attackLog.put("attackName", attack.name());
        attackLog.put("outcome", outcomeName);
        attackLog.put("rollMode", rollMode.name());
        if (reaction) {
            attackLog.put("reaction", true);
        }
        // Standard-action modifiers that shaped this roll (Phase 2.7), for the log narrative.
        if (Boolean.TRUE.equals(attacker.getHidden())) {
            attackLog.put("fromHiding", true);
        }
        if (Boolean.TRUE.equals(attacker.getHelpAdvantage())) {
            attackLog.put("helped", true);
        }
        if (targetDodging) {
            attackLog.put("targetDodging", true);
        }
        if (roll.d20A() != null) {
            attackLog.put("d20A", roll.d20A());
        }
        if (roll.d20B() != null) {
            attackLog.put("d20B", roll.d20B());
        }
        attackLog.put("d20", effectiveD20);
        if (total != null) {
            attackLog.put("total", total);
        }
        if (targetAc != null) {
            attackLog.put("targetAc", targetAc);
        }
        if (attackBonusOut != null) {
            attackLog.put("attackBonus", attackBonusOut);
        }
        if (attack.saveDc() != null) {
            attackLog.put("saveDc", attack.saveDc());
            attackLog.put("saveAbility", saveAbilityOut);
            attackLog.put("saveBonus", saveBonusOut);
            attackLog.put("saveTotal", saveTotalOut);
        }
        if (damage != null) {
            attackLog.put("damage", damage);
        }
        if (damageModifierOut != null && !"NONE".equals(damageModifierOut)) {
            attackLog.put("damageModifier", damageModifierOut);
        }
        if (attack.damageType() != null) {
            attackLog.put("damageType", attack.damageType());
        }
        if (cover != CoverType.NONE) {
            attackLog.put("cover", cover.name());
        }
        if (range.checked()) {
            attackLog.put("distanceFt", range.distanceFt());
            attackLog.put("rangeNote", range.note());
            if (range.forcedDisadvantage() && !gmOverrideRange) {
                attackLog.put("forcedDisadvantage", true);
            }
            if (gmOverrideRange && (range.outOfRange() || range.forcedDisadvantage())) {
                attackLog.put("gmOverrideRange", true);
            }
        }
        battleLogService.append(battleId, campaignId, BattleLogType.ATTACK, attacker.getId(), target.getId(),
                attackLog, BattleLogVisibility.PUBLIC, user.getId());

        if (damage != null && damage > 0) {
            applyDamageOrHeal(target, -damage, user, campaignId);
        }

        // Consume the attacker's one-shot advantage sources (Phase 2.7): attacking reveals a hidden
        // combatant, and an ally's Help is spent on this attack. Persisted by the save below.
        attacker.setHidden(false);
        attacker.setHelpAdvantage(false);
        // Spend the reaction for an opportunity/reaction attack; decrement the Multiattack budget for a
        // multiattacking monster; otherwise spend the action (Phase 2.8 / 2.9).
        if (reaction) {
            attacker.setReactionUsed(true);
        } else if (multiattack) {
            attacker.setAttacksRemaining(attacker.getAttacksRemaining() - 1);
        } else {
            attacker.setActionSpent(attacker.getActionSpent() + 1);
        }
        // Expend a recharge ability so it can't be reused until it recharges at the monster's turn start.
        if (attack.rechargeMin() != null && attack.featureId() != null) {
            markRechargeSpent(attacker, attack.featureId());
        }
        combatantRepository.save(attacker);

        boolean down = target.getCurrentHp() != null && target.getCurrentHp() <= 0;
        log.info("Attack resolved: battleId={}, attacker={}, target={}, attack='{}', mode={}, d20={}, outcome={}, dmg={}, by={}",
                battleId, attacker.getDisplayName(), target.getDisplayName(), attack.name(),
                rollMode, effectiveD20, outcomeName, damage, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("attackerName", attacker.getDisplayName());
        payload.put("targetName", target.getDisplayName());
        payload.put("attackName", attack.name());
        payload.put("outcome", outcomeName);
        if (damage != null) {
            payload.put("damage", damage);
        }
        if (damageModifierOut != null && !"NONE".equals(damageModifierOut)) {
            payload.put("damageModifier", damageModifierOut);
        }
        if (attack.saveDc() != null) {
            payload.put("saveDc", attack.saveDc());
            payload.put("saveAbility", saveAbilityOut);
            payload.put("saveBonus", saveBonusOut);
            payload.put("saveTotal", saveTotalOut);
        }
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        BattleResponse fresh = toResponse(battle, orderedCombatants(battleId));
        return BattleActionResultResponse.builder()
                .attackerCombatantId(attacker.getId())
                .attackerName(attacker.getDisplayName())
                .targetCombatantId(target.getId())
                .targetName(target.getDisplayName())
                .attackName(attack.name())
                .d20(effectiveD20)
                .rollMode(rollMode.name())
                .d20A(roll.d20A())
                .d20B(roll.d20B())
                .effectiveD20(effectiveD20)
                .advantageReason(request.getAdvantageReason())
                .attackBonus(attackBonusOut)
                .total(total)
                .targetAc(targetAc)
                .saveDc(attack.saveDc())
                .saveAbility(saveAbilityOut)
                .saveBonus(saveBonusOut)
                .saveTotal(saveTotalOut)
                .saveRollMode(saveRollModeOut)
                .outcome(outcomeName)
                .damage(damage)
                .damageType(attack.damageType())
                .damageModifier(damageModifierOut)
                .cover(cover != CoverType.NONE ? cover.name() : null)
                .distanceFt(range.distanceFt())
                .rangeNote(range.checked() ? range.note() : null)
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
    }

    /**
     * Легаси-путь использования предмета в бою (лечение из {@code damage_dice} шаблона).
     * <p>При активной item-механике ({@code app.feature-rules.items-enabled}) и наличии у
     * определения предмета approved item-правила возвращает 409 {@code USE_VIA_ABILITY_PATH},
     * чтобы у одного предмета не было двух путей использования: клиент должен использовать
     * предмет через use-ability ({@link ItemAbilityUseService}). Полный демонтаж — вне скоупа
     * (ITEM_ABIL Фаза 3, §4.6; решение Р5).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     * @deprecated используйте use-ability ({@link ItemAbilityUseService#use}) для предметов с
     *             feature-rules; метод сохранён для легаси-потребляемых без правил.
     */
    @Deprecated
    @Transactional
    public BattleActionResultResponse performUseItem(UUID campaignId, UUID battleId,
                                                     BattleUseItemRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Items can only be used in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant actor = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, actor);
        if (actor.getType() != CombatantType.CHARACTER || actor.getCharacter() == null) {
            throw new BadRequestException("Only characters can use items");
        }

        // Lock the actor row and reject a second action this turn (using an item spends the action).
        actor = combatantRepository.findByIdForUpdate(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor combatant not found"));
        if (actor.getActionSpent() >= actor.getActionMax()) {
            throw new BadRequestException("The action has already been used this turn");
        }

        // Lock the item row so the quantity decrement / delete can't double-spend under a race.
        ItemInstance item = itemInstanceRepository.findByIdForUpdate(request.getItemInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(actor.getCharacter().getId())) {
            throw new BadRequestException("This item is not carried by the active character");
        }

        // При активной item-механике предмет с approved-правилом используется только через use-ability.
        if (itemAbilityResolver.hasApprovedItemRule(item)) {
            throw new DuplicateResourceException("USE_VIA_ABILITY_PATH");
        }

        String healExpression = consumableHealExpression(item);
        if (healExpression == null) {
            throw new BadRequestException("This item cannot be used in combat");
        }

        BattleCombatant target = actor;
        if (request.getTargetCombatantId() != null) {
            target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
            if (!target.getBattle().getId().equals(battleId)) {
                throw new BadRequestException("Target does not belong to this battle");
            }
        }

        int healed = diceRoller.rollDamage(healExpression, false);
        applyDamageOrHeal(target, healed, user, campaignId);
        consumeOneUnit(item);

        actor.setActionSpent(actor.getActionSpent() + 1);
        combatantRepository.save(actor);

        log.info("Item used: battleId={}, actor={}, item='{}', target={}, healed={}, by={}",
                battleId, actor.getDisplayName(), item.getDisplayName(), target.getDisplayName(), healed, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("actorName", actor.getDisplayName());
        payload.put("itemName", item.getDisplayName());
        payload.put("targetName", target.getDisplayName());
        payload.put("healed", healed);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        boolean down = target.getCurrentHp() != null && target.getCurrentHp() <= 0;
        BattleResponse fresh = toResponse(battle, orderedCombatants(battleId));
        return BattleActionResultResponse.builder()
                .attackerCombatantId(actor.getId())
                .attackerName(actor.getDisplayName())
                .targetCombatantId(target.getId())
                .targetName(target.getDisplayName())
                .attackName(item.getDisplayName())
                .outcome("ITEM_USED")
                .damage(healed)
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
    }

    /**
     * Restoration dice for a usable consumable, or {@code null} when the item has no in-combat
     * use. A legacy {@link ItemTemplate} that authors damage dice (e.g. a healing potion) exposes
     * them here as the amount restored, combined with its flat bonus.
     */
    private String consumableHealExpression(ItemInstance item) {
        ItemTemplate template = item.getTemplate();
        if (template == null) {
            return null;
        }
        String dice = template.getDamageDice();
        if (dice == null || dice.isBlank()) {
            return null;
        }
        int bonus = template.getDamageBonus() != null ? template.getDamageBonus() : 0;
        return bonus != 0 ? dice + (bonus > 0 ? "+" : "") + bonus : dice;
    }

    /** Spends one unit of an item: decrements the stack, deleting the row when it reaches zero. */
    private void consumeOneUnit(ItemInstance item) {
        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        if (quantity <= 1) {
            itemInstanceRepository.delete(item);
        } else {
            item.setQuantity(quantity - 1);
            itemInstanceRepository.save(item);
        }
    }

    /**
     * Выполняет операции "spend action" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse spendAction(UUID campaignId, UUID battleId, UUID combatantId,
                                      SpendActionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Actions can only be spent in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        // Lock the combatant row so the duplicate-slot check below can't race with a concurrent spend.
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);

        SpendActionRequest.Slot slot = request.getSlot();
        // Actions and bonus actions are spent on the combatant's own turn. Reactions and legendary
        // actions are spent in response to other combatants, so they may be spent at any time.
        if (slot == SpendActionRequest.Slot.ACTION || slot == SpendActionRequest.Slot.BONUS_ACTION) {
            BattleCombatant active = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
            if (!active.getId().equals(combatant.getId())) {
                throw new BadRequestException("An action or bonus action can only be spent on the combatant's own turn");
            }
        }

        switch (slot) {
            case ACTION -> {
                if (combatant.getActionSpent() >= combatant.getActionMax()) {
                    throw new BadRequestException("The action has already been used this turn");
                }
                combatant.setActionSpent(combatant.getActionSpent() + 1);
            }
            case BONUS_ACTION -> {
                if (combatant.getBonusActionSpent() >= combatant.getBonusActionMax()) {
                    throw new BadRequestException("The bonus action has already been used this turn");
                }
                combatant.setBonusActionSpent(combatant.getBonusActionSpent() + 1);
            }
            case LEGENDARY_ACTION -> {
                if (combatant.getLegendaryActionSpent() >= combatant.getLegendaryActionMax()) {
                    throw new BadRequestException("No legendary actions remain this turn");
                }
                combatant.setLegendaryActionSpent(combatant.getLegendaryActionSpent() + 1);
            }
            case REACTION -> {
                if (Boolean.TRUE.equals(combatant.getReactionUsed())) {
                    throw new BadRequestException("The reaction has already been used this turn");
                }
                combatant.setReactionUsed(true);
            }
        }
        combatantRepository.save(combatant);

        log.info("Action economy spent: battleId={}, combatant={}, slot={}, by={}",
                battleId, combatant.getDisplayName(), slot, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    private static final java.util.regex.Pattern LEGENDARY_RESISTANCE = java.util.regex.Pattern.compile(
            "(legendary resistance|легендарн\\w* сопротивлен\\w*)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /** The Legendary Resistance uses/day parsed from a monster's trait text (e.g. "(3/Day)"); 0 if none. */
    private int parseLegendaryResistance(Monster monster) {
        if (monster.getFeatures() == null) {
            return 0;
        }
        for (MonsterFeature f : monster.getFeatures()) {
            String name = (f.getNameRusloc() != null ? f.getNameRusloc() : "")
                    + " " + (f.getNameEngloc() != null ? f.getNameEngloc() : "");
            if (LEGENDARY_RESISTANCE.matcher(name).find()) {
                int[] nums = parseFeet(name + " " + (f.getDescriptionRusloc() != null ? f.getDescriptionRusloc() : ""));
                if (nums.length > 0 && nums[0] > 0 && nums[0] <= 9) {
                    return nums[0];
                }
                return 3; // default per 5e when the count is not written structurally
            }
        }
        return 0;
    }

    // ---- Recharge abilities (Phase 2.9) ----------------------------------------------------------

    /** The set of recharge-ability feature ids currently expended on this combatant. */
    private java.util.Set<UUID> rechargeSpentIds(BattleCombatant combatant) {
        String json = combatant.getRechargeSpentFeatureIds();
        if (json == null || json.isBlank()) {
            return new java.util.HashSet<>();
        }
        try {
            List<String> ids = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            java.util.Set<UUID> out = new java.util.HashSet<>();
            for (String id : ids) {
                out.add(UUID.fromString(id));
            }
            return out;
        } catch (Exception e) {
            return new java.util.HashSet<>();
        }
    }

    private void writeRechargeSpent(BattleCombatant combatant, java.util.Set<UUID> ids) {
        try {
            combatant.setRechargeSpentFeatureIds(ids.isEmpty() ? null
                    : objectMapper.writeValueAsString(ids.stream().map(UUID::toString).toList()));
        } catch (Exception e) {
            combatant.setRechargeSpentFeatureIds(null);
        }
    }

    /** Marks a recharge ability expended (used) so it can't fire again until it recharges. */
    private void markRechargeSpent(BattleCombatant combatant, UUID featureId) {
        java.util.Set<UUID> ids = rechargeSpentIds(combatant);
        ids.add(featureId);
        writeRechargeSpent(combatant, ids);
    }

    /**
     * At the start of a monster's turn, rolls a d6 for each expended recharge ability; on a result of
     * at least its {@code rechargeMin} the ability recharges (leaves the expended set). Returns the
     * names that recharged, for the log.
     */
    private List<String> rollRecharge(BattleCombatant combatant) {
        if (combatant.getType() != CombatantType.MONSTER || combatant.getMonster() == null) {
            return List.of();
        }
        java.util.Set<UUID> spent = rechargeSpentIds(combatant);
        if (spent.isEmpty()) {
            return List.of();
        }
        List<String> recharged = new ArrayList<>();
        for (MonsterFeature f : combatant.getMonster().getFeatures()) {
            if (f.getRechargeMin() == null || !spent.contains(f.getId())) {
                continue;
            }
            if (diceRoller.rollDie(6) >= f.getRechargeMin()) {
                spent.remove(f.getId());
                recharged.add(f.getNameRusloc());
            }
        }
        writeRechargeSpent(combatant, spent);
        return recharged;
    }

    private static final java.util.regex.Pattern MULTIATTACK = java.util.regex.Pattern.compile(
            "(multiattack|мультиатак\\w*)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /** The number of attacks a monster's Multiattack grants (0 = no Multiattack; default 2 if unparseable). */
    private int parseMultiattack(Monster monster) {
        if (monster.getFeatures() == null) {
            return 0;
        }
        for (MonsterFeature f : monster.getFeatures()) {
            String name = (f.getNameRusloc() != null ? f.getNameRusloc() : "")
                    + " " + (f.getNameEngloc() != null ? f.getNameEngloc() : "");
            if (MULTIATTACK.matcher(name).find()) {
                int n = numberWord(f.getDescriptionRusloc());
                return n > 0 ? n : 2;
            }
        }
        return 0;
    }

    /** Extracts an attack count (2–6) from Multiattack prose — a digit or an en/ru number word. */
    private static int numberWord(String text) {
        if (text == null) {
            return 0;
        }
        String s = text.toLowerCase();
        int[] digits = parseFeet(s);
        for (int d : digits) {
            if (d >= 2 && d <= 6) {
                return d;
            }
        }
        if (s.contains("two") || s.contains("две") || s.contains("два")) return 2;
        if (s.contains("three") || s.contains("три")) return 3;
        if (s.contains("four") || s.contains("четыре")) return 4;
        if (s.contains("five") || s.contains("пять")) return 5;
        return 0;
    }

    /**
     * GM spends one of a monster's Legendary Resistance uses to turn a failed save into an automatic
     * success (Phase 2.9). This is a manual override over the 0.4 save mechanic: the GM sees the failed
     * save and clicks to auto-succeed it. Decrements the pool and logs it; rejects when none remain.
     */
    @Transactional
    public BattleResponse useLegendaryResistance(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Legendary Resistance can only be used in an active battle");

        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        int max = nz(combatant.getLegendaryResistanceMax());
        int used = nz(combatant.getLegendaryResistanceUsed());
        if (used >= max) {
            throw new BadRequestException("No Legendary Resistance uses remain");
        }
        combatant.setLegendaryResistanceUsed(used + 1);
        combatantRepository.save(combatant);

        battleLogService.append(battleId, campaignId, BattleLogType.GM_OVERRIDE, combatant.getId(), null,
                Map.of("event", "LEGENDARY_RESISTANCE", "combatant", combatant.getDisplayName(),
                        "remaining", max - (used + 1)),
                BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId, "event", "LEGENDARY_RESISTANCE"), user.getId());

        log.info("Legendary Resistance used: battleId={}, combatant={}, remaining={}, by={}",
                battleId, combatant.getDisplayName(), max - (used + 1), username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * GM hides or reveals a monster's identity in the tracker (Phase 2.10). While hidden, players see
     * a generic public label; the GM always sees the real name. The token's visual visibility on the
     * map is a separate map-side toggle (1.7).
     */
    @Transactional
    public BattleResponse setIdentityHidden(UUID campaignId, UUID battleId, UUID combatantId,
                                            boolean hidden, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        combatant.setIdentityHidden(hidden);
        combatantRepository.save(combatant);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Identity {} for combatant: battleId={}, combatantId={}, by={}",
                hidden ? "hidden" : "revealed", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Устанавливает или снимает ручной GM-override скорости комбатанта (фаза 2.11). При заданном
     * значении оно используется как базовая скорость в бюджете перемещения (для Haste/Slow и правок),
     * не затрагивая immutable-статблок; {@code null} возвращает обычный расчёт скорости. Только GM/админ.
     *
     * @param campaignId  идентификатор кампании, к которой относится бой
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта, чью скорость меняем
     * @param ft          новая скорость в футах (&ge; 0), либо {@code null} чтобы снять override
     * @param username    имя пользователя-инициатора (проверяется на права GM/админа)
     * @return актуальное состояние боя после изменения
     */
    @Transactional
    public BattleResponse setSpeedOverride(UUID campaignId, UUID battleId, UUID combatantId,
                                           Integer ft, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        if (ft != null && ft < 0) {
            throw new BadRequestException("Speed override cannot be negative");
        }
        combatant.setSpeedOverrideFt(ft);
        combatantRepository.save(combatant);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Speed override {} for combatant: battleId={}, combatantId={}, by={}",
                ft == null ? "cleared" : ft + "ft", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /** Chebyshev-дистанция между клетками в футах (5 фт/клетка); null, если любая координата не задана. */
    private static Integer forcedDistanceFt(Integer fromCol, Integer fromRow, Integer toCol, Integer toRow) {
        if (fromCol == null || fromRow == null || toCol == null || toRow == null) {
            return null;
        }
        return Math.max(Math.abs(fromCol - toCol), Math.abs(fromRow - toRow)) * 5;
    }

    /**
     * Принудительно перемещает комбатанта (push/pull/slide, фаза 2.12) без траты его движения и без
     * провокации атак. Позиции клеток приходят с фронта; core проверяет дистанцию против максимума
     * эффекта и передаёт итоговую клетку в map (который исполняет перемещение и рассылает событие).
     * Только GM/админ.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    тип, цель, итоговая клетка и (опц.) исходная клетка + максимум дистанции
     * @param username   инициатор (проверяется на права GM/админа)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse forcedMovement(UUID campaignId, UUID battleId, ForcedMoveRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Forced movement can only happen in an active battle");

        BattleCombatant target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }
        Integer distFt = forcedDistanceFt(request.getFromCol(), request.getFromRow(),
                request.getToCol(), request.getToRow());
        if (request.getMaxDistanceFt() != null && distFt != null && distFt > request.getMaxDistanceFt()) {
            throw new BadRequestException("Forced movement exceeds " + request.getMaxDistanceFt() + " ft");
        }

        mapTokenMover.forcedMove(battleId, new MapTokenMover.ForcedMoveSpec(request.getType().name(),
                List.of(new MapTokenMover.TokenMove(target.getId(), request.getToCol(), request.getToRow()))));

        Map<String, Object> logData = new HashMap<>();
        logData.put("type", request.getType().name());
        logData.put("target", target.getDisplayName());
        if (distFt != null) {
            logData.put("distanceFt", distFt);
        }
        // Обратимость (фаза 3.5): откат вернёт токен в исходную клетку (если она известна).
        Map<String, Object> undo = positionUndo(List.of(
                positionMove(target.getId(), request.getFromCol(), request.getFromRow())));
        battleLogService.append(battleId, campaignId, BattleLogType.FORCED_MOVE, null, target.getId(),
                logData, BattleLogVisibility.PUBLIC, user.getId(), undo);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Forced movement: battleId={}, type={}, target={}, by={}",
                battleId, request.getType(), target.getDisplayName(), username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Телепортирует комбатанта, при необходимости прихватывая ближайших союзников (фаза 2.12 — под
     * заклинания вида «телепорт с союзником»). Core проверяет, что точка назначения в пределах дальности,
     * а каждый союзник — в радиусе прихвата от телепортируемого и его точка тоже в пределах дальности;
     * затем передаёт все перемещения в map. Инициатор — владелец комбатанта или GM.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    инициатор, его точка назначения, дальность/радиус и список прихватываемых союзников
     * @param username   инициатор (владелец комбатанта или GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse teleport(UUID campaignId, UUID battleId, TeleportRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Teleport can only happen in an active battle");

        BattleCombatant caster = combatantRepository.findByIdForUpdate(request.getCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!caster.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, caster);

        Integer destDist = forcedDistanceFt(request.getFromCol(), request.getFromRow(),
                request.getToCol(), request.getToRow());
        if (request.getRangeFt() != null && destDist != null && destDist > request.getRangeFt()) {
            throw new BadRequestException("Teleport destination is beyond range (" + request.getRangeFt() + " ft)");
        }

        List<MapTokenMover.TokenMove> moves = new ArrayList<>();
        moves.add(new MapTokenMover.TokenMove(caster.getId(), request.getToCol(), request.getToRow()));
        List<Map<String, Object>> backMoves = new ArrayList<>();
        backMoves.add(positionMove(caster.getId(), request.getFromCol(), request.getFromRow()));
        int allyCount = 0;
        if (request.getAllies() != null) {
            for (TeleportRequest.Ally ally : request.getAllies()) {
                if (ally.getCombatantId().equals(caster.getId())) {
                    continue;
                }
                BattleCombatant allyC = combatantRepository.findByIdForUpdate(ally.getCombatantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ally combatant not found"));
                if (!allyC.getBattle().getId().equals(battleId)) {
                    throw new BadRequestException("Ally does not belong to this battle");
                }
                Integer pickupDist = forcedDistanceFt(request.getFromCol(), request.getFromRow(),
                        ally.getFromCol(), ally.getFromRow());
                if (request.getAllyPickupFt() != null && pickupDist != null && pickupDist > request.getAllyPickupFt()) {
                    throw new BadRequestException("Ally '" + allyC.getDisplayName() + "' is too far to bring along");
                }
                Integer allyDestDist = forcedDistanceFt(request.getFromCol(), request.getFromRow(),
                        ally.getToCol(), ally.getToRow());
                if (request.getRangeFt() != null && allyDestDist != null && allyDestDist > request.getRangeFt()) {
                    throw new BadRequestException("Ally destination is beyond range (" + request.getRangeFt() + " ft)");
                }
                moves.add(new MapTokenMover.TokenMove(allyC.getId(), ally.getToCol(), ally.getToRow()));
                backMoves.add(positionMove(allyC.getId(), ally.getFromCol(), ally.getFromRow()));
                allyCount++;
            }
        }

        mapTokenMover.forcedMove(battleId, new MapTokenMover.ForcedMoveSpec("TELEPORT", moves));

        Map<String, Object> logData = new HashMap<>();
        logData.put("combatant", caster.getDisplayName());
        logData.put("allies", allyCount);
        // Обратимость (фаза 3.5): откат вернёт всех телепортированных в исходные клетки.
        Map<String, Object> undo = positionUndo(backMoves);
        battleLogService.append(battleId, campaignId, BattleLogType.TELEPORT, caster.getId(), null,
                logData, BattleLogVisibility.PUBLIC, user.getId(), undo);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Teleport: battleId={}, combatant={}, allies={}, by={}",
                battleId, caster.getDisplayName(), allyCount, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Может ли комбатант зависать в воздухе (hover, фаза 2.13): для монстра — есть ли в статблоке
     * скорость с флагом hover. Персонажи hover не имеют.
     *
     * @param combatant комбатант для проверки
     * @return {@code true}, если существо умеет зависать
     */
    private static boolean canHover(BattleCombatant combatant) {
        if (combatant.getType() != CombatantType.MONSTER || combatant.getMonster() == null
                || combatant.getMonster().getSpeeds() == null) {
            return false;
        }
        return combatant.getMonster().getSpeeds().stream()
                .anyMatch(ms -> Boolean.TRUE.equals(ms.getHover()));
    }

    /**
     * Устанавливает или снимает устойчивое состояние полёта комбатанта (фаза 2.13). В отличие от
     * ходовых состояний, полёт сохраняется между ходами. Инициатор — владелец комбатанта или GM.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param flying      {@code true} — поднять в воздух, {@code false} — приземлить
     * @param username    инициатор (владелец комбатанта или GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse setFlying(UUID campaignId, UUID battleId, UUID combatantId,
                                    boolean flying, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);
        combatant.setFlying(flying);
        combatantRepository.save(combatant);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Flying {} for combatant: battleId={}, combatantId={}, by={}",
                flying ? "on" : "off", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Отмечает/снимает внезапность комбатанта (фаза 3.7). Застигнутое врасплох существо в первом раунде
     * не может действовать/реагировать (гвард в spendSlot); флаг автоматически снимается по окончании его
     * первого хода (см. endTurn). GM задаёт внезапных существ в начале боя. Только GM/админ.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param surprised   {@code true} — застигнут врасплох, {@code false} — снять
     * @param username    инициатор (GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse setSurprised(UUID campaignId, UUID battleId, UUID combatantId,
                                       boolean surprised, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        combatant.setSurprised(surprised);
        combatantRepository.save(combatant);
        battleLogService.append(battleId, campaignId, BattleLogType.SURPRISE, null, combatant.getId(),
                Map.of("combatantName", combatant.getDisplayName(), "surprised", surprised),
                BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Surprise {} for combatant: battleId={}, combatantId={}, by={}",
                surprised ? "set" : "cleared", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Подготавливает действие (Ready, фаза 3.7): комбатант тратит своё действие, чтобы отложить его до
     * триггера (описание — свободный текст). Реюзает экономию действий (spendSlot ACTION), поэтому
     * застигнутый/уже потративший действие получит отказ. Инициатор — контролёр комбатанта или GM.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта, который готовит действие
     * @param request     описание подготовленного действия и его триггера
     * @param username    инициатор (контролёр комбатанта или GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse readyAction(UUID campaignId, UUID battleId, UUID combatantId,
                                      ReadyActionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Actions can only be readied in an active battle");
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);

        spendSlot(combatant, SpendActionRequest.Slot.ACTION); // подготовка тратит действие
        combatant.setReadiedAction(request.getDescription());
        combatantRepository.save(combatant);
        battleLogService.append(battleId, campaignId, BattleLogType.READY, combatant.getId(), null,
                Map.of("combatantName", combatant.getDisplayName(), "action", "DECLARED",
                        "description", request.getDescription()),
                BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Ready declared: battleId={}, combatantId={}, by={}", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Срабатывание подготовленного действия (Ready, фаза 3.7) по триггеру: тратит реакцию комбатанта
     * (реюз слота REACTION 2.8), очищает подготовленное действие и логирует срабатывание. Требует, чтобы
     * действие было подготовлено и реакция ещё не потрачена. Инициатор — контролёр комбатанта или GM.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта, чьё подготовленное действие срабатывает
     * @param username    инициатор (контролёр комбатанта или GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse triggerReady(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);
        if (combatant.getReadiedAction() == null) {
            throw new BadRequestException("No readied action to trigger");
        }

        spendSlot(combatant, SpendActionRequest.Slot.REACTION); // срабатывание тратит реакцию
        String description = combatant.getReadiedAction();
        combatant.setReadiedAction(null);
        combatantRepository.save(combatant);
        battleLogService.append(battleId, campaignId, BattleLogType.READY, combatant.getId(), null,
                Map.of("combatantName", combatant.getDisplayName(), "action", "TRIGGERED",
                        "description", description),
                BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Ready triggered: battleId={}, combatantId={}, by={}", battleId, combatantId, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Срабатывание ловушки по цели (фаза 3.2): переиспользует примитивы сейва/митигации/HP (как bulkAction)
     * — цель кидает спасбросок (ручной d20 или AUTO) против DC ловушки, урон полный/половинный/0, применяется
     * сопротивление, пишется лог {@code TRAP}. Параметры ловушки приходят с карты через фронт (A1). Только GM.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    цель, урон и параметры спасброска ловушки
     * @param username   инициатор (GM/админ)
     * @return актуальное состояние боя
     */
    @Transactional
    public BattleResponse triggerTrap(UUID campaignId, UUID battleId, TrapTriggerRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Traps only trigger in an active battle");

        BattleCombatant target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }

        int dmg = nz(request.getAmount());
        String saveOutcome = null;
        Integer saveTotal = null;
        if (request.getSaveDc() != null && request.getSaveAbility() != null) {
            int d20 = request.getSaveD20() != null ? request.getSaveD20() : diceRoller.rollD20();
            int bonus = resolveTargetSaveBonus(target, request.getSaveAbility());
            AttackResolver.SaveOutcome save = AttackResolver.resolveSave(d20, bonus, request.getSaveDc());
            saveOutcome = save.name();
            saveTotal = d20 + bonus;
            if (save == AttackResolver.SaveOutcome.SUCCESS) {
                dmg = Boolean.TRUE.equals(request.getHalfOnSave()) ? dmg / 2 : 0;
            }
        }
        String damageModifier = null;
        if (dmg > 0) {
            DamageMitigationService.Mitigation mit = damageMitigationService.mitigate(target, dmg, request.getDamageTypeId());
            dmg = mit.finalDamage();
            damageModifier = mit.modifier().name();
            if (dmg > 0) {
                applyDamageOrHeal(target, -dmg, user, campaignId);
            }
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("target", target.getDisplayName());
        if (request.getLabel() != null) {
            logData.put("label", request.getLabel());
        }
        if (saveOutcome != null) {
            logData.put("save", saveOutcome);
            logData.put("saveTotal", saveTotal);
            logData.put("saveDc", request.getSaveDc());
        }
        logData.put("damage", dmg);
        if (damageModifier != null && !"NONE".equals(damageModifier)) {
            logData.put("damageModifier", damageModifier);
        }
        battleLogService.append(battleId, campaignId, BattleLogType.TRAP, null, target.getId(),
                logData, BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Trap triggered: battleId={}, target={}, damage={}, by={}",
                battleId, target.getDisplayName(), dmg, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Падение комбатанта с высоты (фаза 3.4). Урон — 1к6 за каждые 10 футов (кап 20к6); при приземлении
     * цель обычно валится ничком (prone). Реюзает {@code applyDamageOrHeal} и
     * {@code ConditionService.applyByCode("prone")}, пишет лог {@code FALL}. Если комбатант летел — падение
     * сбрасывает флаг полёта (2.13). Урон берётся из {@code manualTotal} (GM/фронт уже бросил) либо
     * бросается сервером (к6 по высоте). Права — контроль над комбатантом или GM (как у {@code setFlying}).
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    падающий комбатант, высота, готовый урон и флаг prone
     * @param username   инициатор (контролёр комбатанта или GM/админ)
     * @return актуальное состояние боя после падения
     */
    @Transactional
    public BattleResponse fall(UUID campaignId, UUID battleId, FallRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(request.getCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);

        int heightFt = Math.max(0, nz(request.getHeightFt()));
        int dice = Math.min(20, heightFt / 10); // 1к6 за каждые 10 футов, кап 20к6
        int total;
        if (request.getManualTotal() != null) {
            total = Math.max(0, request.getManualTotal());
        } else {
            int sum = 0;
            for (int i = 0; i < dice; i++) {
                sum += diceRoller.rollDie(6);
            }
            total = sum;
        }

        // Падение прекращает полёт (2.13).
        if (Boolean.TRUE.equals(combatant.getFlying())) {
            combatant.setFlying(false);
        }
        if (total > 0) {
            applyDamageOrHeal(combatant, -total, user, campaignId); // сохраняет комбатанта (в т.ч. сброс полёта)
        } else {
            combatantRepository.save(combatant); // урона нет — сохраняем хотя бы сброс полёта
        }

        boolean prone = !Boolean.FALSE.equals(request.getApplyProne()) && heightFt >= 10;
        if (prone) {
            conditionService.applyByCode(campaignId, combatant, "prone", user.getId(), battle.getRoundNumber());
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("target", combatant.getDisplayName());
        logData.put("heightFt", heightFt);
        logData.put("dice", dice);
        logData.put("damage", total);
        if (prone) {
            logData.put("condition", "prone");
        }
        battleLogService.append(battleId, campaignId, BattleLogType.FALL, null, combatant.getId(),
                logData, BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Fall: battleId={}, combatant={}, heightFt={}, damage={}, by={}",
                battleId, combatant.getDisplayName(), heightFt, total, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Откат последней обратимой операции боя (фаза 3.5). Находит самую свежую запись журнала с «обратной
     * дельтой» (HP / условие / позиция), применяет обратное действие, помечает запись откатанной (повтор
     * запрещён) и пишет запись {@code UNDO}. Только GM/админ (инструмент коррекции мастера).
     *
     * <ul>
     *   <li>HP — применяет обратную дельту к комбатанту (тихо, без новой обратимой записи);</li>
     *   <li>условие — снимает добавленное состояние с комбатанта;</li>
     *   <li>позиция — возвращает токен(ы) в исходные клетки через map (реюз {@code MapTokenMover}).</li>
     * </ul>
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param username   инициатор (GM/админ)
     * @return актуальное состояние боя после отката
     */
    @Transactional
    public BattleResponse undo(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);

        BattleLog entry = battleLogService.findLastUndoable(battleId)
                .orElseThrow(() -> new BadRequestException("Nothing to undo"));
        Map<String, Object> undo = parseUndoPayload(entry.getUndoPayload());
        String kind = undo == null ? null : String.valueOf(undo.get("kind"));
        if (kind == null) {
            throw new BadRequestException("Nothing to undo");
        }

        Map<String, Object> undoLog = new HashMap<>();
        undoLog.put("undoneSeq", entry.getSeq());
        undoLog.put("kind", kind);
        switch (kind) {
            case "HP" -> {
                BattleCombatant c = combatantRepository.findByIdForUpdate(UUID.fromString((String) undo.get("combatantId")))
                        .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
                int delta = ((Number) undo.get("delta")).intValue();
                applyDamageOrHeal(c, -delta, user, campaignId, true); // тихо: откат не создаёт новую обратимую запись
                undoLog.put("target", c.getDisplayName());
                undoLog.put("restoredDelta", -delta);
            }
            case "CONDITION_ADD" -> {
                UUID combatantId = UUID.fromString((String) undo.get("combatantId"));
                UUID conditionId = UUID.fromString((String) undo.get("conditionId"));
                conditionService.remove(campaignId, combatantId, conditionId, user.getId());
                undoLog.put("condition", conditionId.toString());
            }
            case "POSITION" -> {
                List<MapTokenMover.TokenMove> back = new ArrayList<>();
                Object rawMoves = undo.get("moves");
                if (rawMoves instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            back.add(new MapTokenMover.TokenMove(
                                    UUID.fromString(String.valueOf(m.get("combatantId"))),
                                    ((Number) m.get("x")).intValue(),
                                    ((Number) m.get("y")).intValue()));
                        }
                    }
                }
                if (back.isEmpty()) {
                    throw new BadRequestException("This move cannot be undone (origin unknown)");
                }
                mapTokenMover.forcedMove(battleId, new MapTokenMover.ForcedMoveSpec("TELEPORT", back));
                undoLog.put("tokens", back.size());
            }
            default -> throw new BadRequestException("Unsupported undo kind: " + kind);
        }

        battleLogService.markUndone(entry);
        battleLogService.append(battleId, campaignId, BattleLogType.UNDO, null, null,
                undoLog, BattleLogVisibility.PUBLIC, user.getId());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        log.info("Undo: battleId={}, kind={}, undoneSeq={}, by={}", battleId, kind, entry.getSeq(), username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /** Разбирает JSON «обратной дельты» записи журнала в Map (фаза 3.5); null при пустом/битом JSON. */
    private Map<String, Object> parseUndoPayload(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse undo payload: {}", ex.getMessage());
            return null;
        }
    }

    /** Одно «обратное» перемещение {combatantId, x, y} для позиционного отката (фаза 3.5); null если клетка неизвестна. */
    private static Map<String, Object> positionMove(UUID combatantId, Integer x, Integer y) {
        if (x == null || y == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("combatantId", combatantId.toString());
        m.put("x", x);
        m.put("y", y);
        return m;
    }

    /** Собирает undo_payload позиционного отката из «обратных» перемещений (фаза 3.5); null если возвращать некуда. */
    private static Map<String, Object> positionUndo(List<Map<String, Object>> moves) {
        List<Map<String, Object>> valid = moves.stream().filter(java.util.Objects::nonNull).toList();
        if (valid.isEmpty()) {
            return null;
        }
        Map<String, Object> undo = new HashMap<>();
        undo.put("kind", "POSITION");
        undo.put("moves", valid);
        return undo;
    }

    /** Spends one action-economy slot on the combatant, rejecting if that slot is already used this turn. */
    private void spendSlot(BattleCombatant combatant, SpendActionRequest.Slot slot) {
        // Внезапность (3.7): застигнутое существо не может тратить слоты действий/реакций.
        if (Boolean.TRUE.equals(combatant.getSurprised())) {
            throw new BadRequestException("The combatant is surprised and cannot act this round");
        }
        switch (slot) {
            case ACTION -> {
                if (combatant.getActionSpent() >= combatant.getActionMax()) {
                    throw new BadRequestException("The action has already been used this turn");
                }
                combatant.setActionSpent(combatant.getActionSpent() + 1);
            }
            case BONUS_ACTION -> {
                if (combatant.getBonusActionSpent() >= combatant.getBonusActionMax()) {
                    throw new BadRequestException("The bonus action has already been used this turn");
                }
                combatant.setBonusActionSpent(combatant.getBonusActionSpent() + 1);
            }
            case LEGENDARY_ACTION -> {
                if (combatant.getLegendaryActionSpent() >= combatant.getLegendaryActionMax()) {
                    throw new BadRequestException("No legendary actions remain this turn");
                }
                combatant.setLegendaryActionSpent(combatant.getLegendaryActionSpent() + 1);
            }
            case REACTION -> {
                if (Boolean.TRUE.equals(combatant.getReactionUsed())) {
                    throw new BadRequestException("The reaction has already been used this turn");
                }
                combatant.setReactionUsed(true);
            }
        }
    }

    /**
     * Выполняет операции "standard action" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse standardAction(UUID campaignId, UUID battleId, UUID combatantId,
                                         StandardActionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Standard actions can only be taken in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);

        BattleCombatant active = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        if (!active.getId().equals(combatant.getId())) {
            throw new BadRequestException("A standard action can only be taken on the combatant's own turn");
        }

        SpendActionRequest.Slot slot = request.getSlot() != null ? request.getSlot() : SpendActionRequest.Slot.ACTION;
        if (slot != SpendActionRequest.Slot.ACTION && slot != SpendActionRequest.Slot.BONUS_ACTION) {
            throw new BadRequestException("A standard action uses an action or a bonus action");
        }
        spendSlot(combatant, slot);

        Map<String, Object> logData = new HashMap<>();
        logData.put("action", request.getType().name());
        logData.put("actor", combatant.getDisplayName());
        logData.put("slot", slot.name());
        UUID logTargetId = null;

        switch (request.getType()) {
            case DASH -> {
                combatant.setDashing(true);
                logData.put("movementBudgetFt", baseSpeedFt(combatant) * 2);
            }
            case DODGE -> combatant.setDodging(true);
            case DISENGAGE -> combatant.setDisengaged(true);
            case HELP -> {
                if (request.getTargetCombatantId() == null) {
                    throw new BadRequestException("Help requires a target ally");
                }
                if (request.getTargetCombatantId().equals(combatant.getId())) {
                    throw new BadRequestException("A combatant cannot Help itself");
                }
                BattleCombatant ally = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Help target not found"));
                if (!ally.getBattle().getId().equals(battleId)) {
                    throw new BadRequestException("Help target does not belong to this battle");
                }
                ally.setHelpAdvantage(true);
                combatantRepository.save(ally);
                logTargetId = ally.getId();
                logData.put("target", ally.getDisplayName());
            }
            case HIDE -> {
                int bonus = nz(request.getStealthBonus());
                int d20 = request.getStealthD20() != null ? request.getStealthD20() : diceRoller.rollD20();
                int total = d20 + bonus;
                boolean success = request.getHideDc() == null || total >= request.getHideDc();
                combatant.setHidden(success);
                logData.put("stealthRoll", d20);
                logData.put("stealthTotal", total);
                if (request.getHideDc() != null) {
                    logData.put("hideDc", request.getHideDc());
                }
                logData.put("success", success);
            }
        }
        combatantRepository.save(combatant);

        battleLogService.append(battleId, campaignId, BattleLogType.STANDARD_ACTION, combatant.getId(), logTargetId,
                logData, BattleLogVisibility.PUBLIC, user.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("action", request.getType().name());
        payload.put("actorName", combatant.getDisplayName());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId, payload, user.getId());

        log.info("Standard action: battleId={}, combatant={}, type={}, slot={}, by={}",
                battleId, combatant.getDisplayName(), request.getType(), slot, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Выполняет операции "contest" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ContestResultResponse contest(UUID campaignId, UUID battleId, UUID combatantId,
                                         ContestRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Contests can only happen in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant actor = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!actor.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, actor);

        BattleCombatant active = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        if (!active.getId().equals(actor.getId())) {
            throw new BadRequestException("A contest can only be initiated on the actor's own turn");
        }
        if (request.getTargetCombatantId().equals(actor.getId())) {
            throw new BadRequestException("A combatant cannot contest itself");
        }
        BattleCombatant target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }

        spendSlot(actor, SpendActionRequest.Slot.ACTION);

        int atkD20 = request.getAttackerD20() != null ? request.getAttackerD20() : diceRoller.rollD20();
        int tgtD20 = request.getTargetD20() != null ? request.getTargetD20() : diceRoller.rollD20();
        int atkTotal = atkD20 + nz(request.getAttackerBonus());
        int tgtTotal = tgtD20 + nz(request.getTargetBonus());
        boolean attackerWins = atkTotal > tgtTotal; // the defender wins ties (5e)

        String appliedCondition = null;
        if (attackerWins) {
            if (request.getType() == ContestType.GRAPPLE) {
                appliedCondition = "grappled";
            } else if (!"PUSH".equalsIgnoreCase(request.getShoveMode())) {
                appliedCondition = "prone"; // PUSH is forced movement handled by map (2.12)
            }
            if (appliedCondition != null) {
                conditionService.applyByCode(campaignId, target, appliedCondition, user.getId(), battle.getRoundNumber());
            }
        }
        combatantRepository.save(actor);

        Map<String, Object> logData = new HashMap<>();
        logData.put("contest", request.getType().name());
        logData.put("attacker", actor.getDisplayName());
        logData.put("target", target.getDisplayName());
        logData.put("attackerRoll", atkD20);
        logData.put("attackerTotal", atkTotal);
        logData.put("targetRoll", tgtD20);
        logData.put("targetTotal", tgtTotal);
        logData.put("attackerWins", attackerWins);
        if (appliedCondition != null) {
            logData.put("condition", appliedCondition);
        }
        if (request.getType() == ContestType.SHOVE && "PUSH".equalsIgnoreCase(request.getShoveMode())) {
            logData.put("shoveMode", "PUSH");
        }
        battleLogService.append(battleId, campaignId, BattleLogType.CONTEST, actor.getId(), target.getId(),
                logData, BattleLogVisibility.PUBLIC, user.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("contest", request.getType().name());
        payload.put("attackerWins", attackerWins);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId, payload, user.getId());

        log.info("Contest: battleId={}, {} by {} vs {} → attackerWins={}, by={}",
                battleId, request.getType(), actor.getDisplayName(), target.getDisplayName(), attackerWins, username);

        return ContestResultResponse.builder()
                .type(request.getType().name())
                .attackerName(actor.getDisplayName())
                .targetName(target.getDisplayName())
                .attackerRoll(atkD20)
                .attackerTotal(atkTotal)
                .targetRoll(tgtD20)
                .targetTotal(tgtTotal)
                .attackerWins(attackerWins)
                .condition(appliedCondition)
                .battle(toResponse(battle, orderedCombatants(battleId)))
                .build();
    }

    /**
     * Выполняет операции "apply combatant hp" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse applyCombatantHp(UUID campaignId, UUID battleId, UUID combatantId,
                                           ApplyCombatantHpRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "HP can only be adjusted in an active battle");

        // Lock the combatant row so a manual HP edit and an attack on the same target accumulate.
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }

        applyDamageOrHeal(combatant, request.getDelta(), user, campaignId);

        log.info("Combatant HP adjusted: battleId={}, combatant={}, delta={}, newHp={}, by={}",
                battleId, combatant.getDisplayName(), request.getDelta(), combatant.getCurrentHp(), username);
        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("targetName", combatant.getDisplayName());
        payload.put("delta", request.getDelta());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Выполняет операции "adjust action economy" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse adjustActionEconomy(UUID campaignId, UUID battleId, UUID combatantId,
                                              AdjustActionEconomyRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Action economy can only be adjusted in an active battle");

        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }

        if (request.getActionMax() != null) {
            combatant.setActionMax(request.getActionMax());
            combatant.setActionSpent(Math.min(nz(combatant.getActionSpent()), request.getActionMax()));
        }
        if (request.getBonusActionMax() != null) {
            combatant.setBonusActionMax(request.getBonusActionMax());
            combatant.setBonusActionSpent(Math.min(nz(combatant.getBonusActionSpent()), request.getBonusActionMax()));
        }
        if (request.getLegendaryActionMax() != null) {
            combatant.setLegendaryActionMax(request.getLegendaryActionMax());
            combatant.setLegendaryActionSpent(
                    Math.min(nz(combatant.getLegendaryActionSpent()), request.getLegendaryActionMax()));
        }
        combatantRepository.save(combatant);

        log.info("Action economy adjusted: battleId={}, combatant={}, action={}, bonus={}, legendary={}, by={}",
                battleId, combatant.getDisplayName(), combatant.getActionMax(),
                combatant.getBonusActionMax(), combatant.getLegendaryActionMax(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId,
                Map.of("battleId", battleId, "combatantId", combatantId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * A combatant's usable attack reduced to the numbers the resolver needs. {@code saveDc} is set
     * only for save-based attacks (the target rolls a saving throw instead of the attacker rolling
     * to hit); it stays null for ordinary attack-roll strikes.
     */
    private record AttackOption(String name, int attackBonus, String damage, String damageType,
                                UUID damageTypeId, Integer saveDc, String saveAbilityCode,
                                boolean ranged, Integer reachFt, Integer rangeNormalFt, Integer rangeLongFt,
                                UUID featureId, Integer rechargeMin) {
    }

    /** Distance/reach gate for an attack (Phase 2.5): whether it was checked, the distance, and its consequences. */
    private record RangeEvaluation(boolean checked, Integer distanceFt, boolean outOfRange,
                                   boolean forcedDisadvantage, String note) {
    }

    private static final java.util.regex.Pattern FEET_NUMBER = java.util.regex.Pattern.compile("\\d+");

    /** Extracts the numbers from a reach/range hint like "5 фт." or "20/60 фт." (feet). */
    private static int[] parseFeet(String text) {
        if (text == null || text.isBlank()) {
            return new int[0];
        }
        java.util.regex.Matcher m = FEET_NUMBER.matcher(text);
        List<Integer> nums = new ArrayList<>();
        while (m.find()) {
            nums.add(Integer.parseInt(m.group()));
        }
        return nums.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Range/reach gate (Phase 2.5). Positions come from the request (map-authoritative, relayed by FE);
     * distance is Chebyshev grid squares × 5 ft. Melee beyond reach or a shot beyond long range is
     * out of range; long range and shooting while threatened in melee force disadvantage. All-null
     * coords → unchecked (backward compatible).
     */
    private RangeEvaluation evaluateRange(BattleAttackRequest req, AttackOption attack) {
        Integer ac = req.getAttackerCol();
        Integer ar = req.getAttackerRow();
        Integer tc = req.getTargetCol();
        Integer tr = req.getTargetRow();
        if (ac == null || ar == null || tc == null || tr == null) {
            return new RangeEvaluation(false, null, false, false, null);
        }
        // 3D-дистанция (фаза 2.13): при заданных высотах разница высот входит в Chebyshev (клетки высоты),
        // чтобы летающие цели считались дальше. Без высот — обычная 2D-дистанция.
        int elevCells = 0;
        if (req.getAttackerElevationFt() != null && req.getTargetElevationFt() != null) {
            elevCells = Math.round(Math.abs(req.getAttackerElevationFt() - req.getTargetElevationFt()) / 5.0f);
        }
        int distFt = Math.max(Math.max(Math.abs(ac - tc), Math.abs(ar - tr)), elevCells) * 5;
        if (!attack.ranged()) {
            int reach = attack.reachFt() != null ? attack.reachFt() : 5;
            boolean out = distFt > reach;
            return new RangeEvaluation(true, distFt, out, false, out ? "OUT_OF_REACH" : "IN_REACH");
        }
        int normal = attack.rangeNormalFt() != null ? attack.rangeNormalFt() : 0;
        int longRange = attack.rangeLongFt() != null ? attack.rangeLongFt() : normal;
        if (normal > 0 && distFt > longRange) {
            return new RangeEvaluation(true, distFt, true, false, "BEYOND_LONG_RANGE");
        }
        boolean beyondNormal = normal > 0 && distFt > normal;
        boolean inMelee = Boolean.TRUE.equals(req.getAttackerInMeleeThreat());
        boolean disadvantage = beyondNormal || inMelee;
        String note = inMelee ? "RANGED_IN_MELEE" : beyondNormal ? "LONG_RANGE" : "IN_RANGE";
        return new RangeEvaluation(true, distFt, false, disadvantage, note);
    }

    /** Short reach/range label for out-of-range error messages. */
    private static String rangeLabel(AttackOption attack) {
        if (!attack.ranged()) {
            return "reach " + (attack.reachFt() != null ? attack.reachFt() : 5) + " ft";
        }
        Integer n = attack.rangeNormalFt();
        Integer l = attack.rangeLongFt();
        return "range " + (n != null ? n : "?") + "/" + (l != null ? l : (n != null ? n : "?")) + " ft";
    }

    /** The two dice considered and the single die selected for the attack per its roll mode. */
    private record RollResolution(Integer d20A, Integer d20B, int effectiveD20) {
    }

    /**
     * Turns the request's roll mode and dice into the effective d20. Manual dice for
     * ADVANTAGE/DISADVANTAGE must come as a {@code d20A}/{@code d20B} pair (the server keeps the
     * higher / lower); a lone legacy {@code d20} is accepted only for NORMAL. When no dice are
     * supplied the server rolls virtually: one die for NORMAL, two (keep higher/lower) otherwise.
     * Policy: advantage/disadvantage with a single manual value is rejected for strictness.
     */
    private RollResolution resolveAttackRoll(BattleAttackRequest request) {
        return resolveRoll("attack", request.getRollMode(), request.getD20(), request.getD20A(), request.getD20B());
    }

    /**
     * Turns a roll mode and its dice into the effective d20 — shared by attack rolls and saving
     * throws. Manual ADVANTAGE/DISADVANTAGE needs a {@code d20A}/{@code d20B} pair (the server keeps
     * the higher/lower); a lone value is accepted only for NORMAL; with no dice the server rolls
     * virtually (one die for NORMAL, two otherwise). {@code label} only shapes the error messages.
     */
    private RollResolution resolveRoll(String label, AttackRollMode mode, Integer single, Integer a, Integer b) {
        AttackRollMode m = mode != null ? mode : AttackRollMode.NORMAL;
        if ((a == null) != (b == null)) {
            throw new BadRequestException("Provide both " + label + " dice (d20A and d20B) together, or neither");
        }
        boolean hasPair = a != null;
        if (m == AttackRollMode.NORMAL) {
            if (hasPair) {
                throw new BadRequestException("A NORMAL " + label + " roll expects a single die, not a d20A/d20B pair");
            }
            int value = single != null ? single : diceRoller.rollD20();
            return new RollResolution(value, null, value);
        }
        if (!hasPair) {
            if (single != null) {
                throw new BadRequestException(m + " " + label + " requires both d20A and d20B (two dice)");
            }
            a = diceRoller.rollD20();
            b = diceRoller.rollD20();
        }
        int effective = m == AttackRollMode.ADVANTAGE ? Math.max(a, b) : Math.min(a, b);
        return new RollResolution(a, b, effective);
    }

    // ---- Saving-throw resolution -----------------------------------------------------------------

    /**
     * The target's saving-throw bonus for the given save ability (a bestiary ability code such as
     * {@code DEXTERITY}). Monster: its statblock save for that ability if present, else the ability
     * modifier from its score. Character: ability modifier + proficiency bonus when proficient in
     * that save + any active-effect save modifiers. Unknown/absent ability → 0.
     */
    private int resolveTargetSaveBonus(BattleCombatant target, String abilityCode) {
        if (abilityCode == null) {
            return 0;
        }
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null) {
            Monster monster = target.getMonster();
            Optional<Short> statblockSave = monster.getSavingThrows().stream()
                    .filter(st -> st.getAbility() != null && abilityCode.equalsIgnoreCase(st.getAbility().getCode()))
                    .map(MonsterSavingThrow::getBonus)
                    .filter(bonus -> bonus != null)
                    .findFirst();
            if (statblockSave.isPresent()) {
                return statblockSave.get().intValue();
            }
            Integer score = monsterAbilityScore(monster, abilityCode);
            return score != null ? CombatCalculator.abilityModifier(score) : 0;
        }
        if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null) {
            PlayerCharacter character = target.getCharacter();
            String slug = canonicalAbilitySlug(abilityCode);
            CharacterStat stat = character.getStats().stream()
                    .filter(s -> s.getStatType() != null && slug != null
                            && slug.equalsIgnoreCase(s.getStatType().getSlug()))
                    .findFirst()
                    .orElse(null);
            if (stat == null) {
                return 0;
            }
            int base = stat.getValue() != null ? CombatCalculator.abilityModifier(stat.getValue()) : 0;
            int proficiency = isProficientSave(character, stat.getStatType().getId())
                    ? proficiencyBonus(nz(character.getTotalLevel())) : 0;
            int effects = modifierAggregator.totalFor(character.getId(),
                    ModifierTarget.save(stat.getStatType().getId(), stat.getStatType().getSlug()));
            return base + proficiency + effects;
        }
        return 0;
    }

    /** Whether the character is proficient in the save for the given ability stat id. */
    private boolean isProficientSave(PlayerCharacter character, UUID statTypeId) {
        String json = character.getSavingThrowProficiencyStatIdsJson();
        if (json == null || json.isBlank() || statTypeId == null) {
            return false;
        }
        try {
            List<String> ids = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return ids.contains(statTypeId.toString());
        } catch (Exception e) {
            return false;
        }
    }

    /** D&D proficiency bonus for a level: 2 + floor((level - 1) / 4). */
    private static int proficiencyBonus(int level) {
        return 2 + (Math.max(1, level) - 1) / 4;
    }

    /** A monster's ability score for a bestiary ability code (its scores live on the statblock). */
    private static Integer monsterAbilityScore(Monster monster, String abilityCode) {
        Short score = switch (abilityCode == null ? "" : abilityCode.toUpperCase()) {
            case "STRENGTH" -> monster.getStrScore();
            case "DEXTERITY" -> monster.getDexScore();
            case "CONSTITUTION" -> monster.getConScore();
            case "INTELLIGENCE" -> monster.getIntScore();
            case "WISDOM" -> monster.getWisScore();
            case "CHARISMA" -> monster.getChaScore();
            default -> null;
        };
        return score != null ? score.intValue() : null;
    }

    /** Maps a bestiary ability code (e.g. {@code DEXTERITY}) to the 3-letter character stat slug ({@code dex}). */
    private static String canonicalAbilitySlug(String abilityCode) {
        if (abilityCode == null) {
            return null;
        }
        return switch (abilityCode.toUpperCase()) {
            case "STRENGTH" -> "str";
            case "DEXTERITY" -> "dex";
            case "CONSTITUTION" -> "con";
            case "INTELLIGENCE" -> "int";
            case "WISDOM" -> "wis";
            case "CHARISMA" -> "cha";
            default -> abilityCode.length() >= 3 ? abilityCode.substring(0, 3).toLowerCase() : abilityCode.toLowerCase();
        };
    }

    /** Human-readable Russian ability name for the save log/UI, from a bestiary ability code. */
    private static String saveAbilityDisplayName(String abilityCode) {
        if (abilityCode == null) {
            return null;
        }
        return switch (abilityCode.toUpperCase()) {
            case "STRENGTH" -> "Сила";
            case "DEXTERITY" -> "Ловкость";
            case "CONSTITUTION" -> "Телосложение";
            case "INTELLIGENCE" -> "Интеллект";
            case "WISDOM" -> "Мудрость";
            case "CHARISMA" -> "Харизма";
            default -> abilityCode;
        };
    }

    /**
     * A character's full attack list: weapon-driven attacks (incl. throw/two-handed variants),
     * progression-based class attacks (class features that deal damage) and any manually-authored
     * attacks from the sheet.
     */
    private List<CharacterAttackResponse> characterAttackList(PlayerCharacter character) {
        List<CharacterAttackResponse> list = new ArrayList<>(weaponAttackService.computeAttacks(character));
        list.addAll(classAbilityCombatService.classAttacks(character));
        String json = character.getAttacksJson();
        if (json != null && !json.isBlank()) {
            try {
                list.addAll(objectMapper.readValue(json, new TypeReference<List<CharacterAttackResponse>>() {}));
            } catch (Exception e) {
                throw new BadRequestException("Could not read this character's attacks", e);
            }
        }
        return list;
    }

    /** Finds the named attack on the active combatant — character weapon/json attacks or monster features. */
    private AttackOption resolveAttack(BattleCombatant attacker, String attackName) {
        if (attacker.getType() == CombatantType.CHARACTER && attacker.getCharacter() != null) {
            List<CharacterAttackResponse> attacks = characterAttackList(attacker.getCharacter());
            if (attacks.isEmpty()) {
                throw new BadRequestException("This character has no attacks");
            }
            return attacks.stream()
                    .filter(a -> a.getName() != null && a.getName().equalsIgnoreCase(attackName))
                    .findFirst()
                    .map(a -> {
                        boolean ranged = "RANGED".equalsIgnoreCase(a.getCategory())
                                || "THROWN".equalsIgnoreCase(a.getCategory());
                        int[] feet = parseFeet(a.getRange());
                        Integer reachFt = null;
                        Integer rangeNormalFt = null;
                        Integer rangeLongFt = null;
                        if (ranged) {
                            rangeNormalFt = feet.length > 0 ? Integer.valueOf(feet[0]) : null;
                            rangeLongFt = feet.length > 1 ? Integer.valueOf(feet[1]) : rangeNormalFt;
                        } else {
                            reachFt = feet.length > 0 ? Integer.valueOf(feet[0]) : Integer.valueOf(5);
                        }
                        return new AttackOption(a.getName(),
                                AttackResolver.parseAttackBonus(a.getAttackBonus()),
                                a.getDamage(), a.getDamageType(), null, null, null,
                                ranged, reachFt, rangeNormalFt, rangeLongFt, null, null);
                    })
                    .orElseThrow(() -> new BadRequestException("Attack '" + attackName + "' not found on this character"));
        }

        if (attacker.getType() == CombatantType.MONSTER && attacker.getMonster() != null) {
            MonsterFeature feature = attacker.getMonster().getFeatures().stream()
                    .filter(f -> f.getAttackType() != null && f.getNameRusloc() != null
                            && f.getNameRusloc().equalsIgnoreCase(attackName))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Attack '" + attackName + "' not found on this monster"));
            int bonus = feature.getAttackBonus() != null ? feature.getAttackBonus() : 0;
            Optional<FeatureDamage> primary = feature.getDamages().stream()
                    .min(Comparator.comparing(FeatureDamage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
            String dice = primary.map(FeatureDamage::getDice)
                    .filter(d -> d != null && !d.isBlank())
                    .orElse(null);
            String damageType = primary
                    .map(d -> d.getDamageType() != null ? d.getDamageType().getNameRusloc() : null)
                    .orElse(null);
            // Structured damage type id (unified damage_type) — lets the target's resistances apply.
            UUID damageTypeId = primary
                    .map(d -> d.getDamageType() != null ? d.getDamageType().getId() : null)
                    .orElse(null);
            // Weapon attacks store their damage inline in the description, not in structured
            // feature_damages rows — fall back to parsing the dice out of the text.
            if (dice == null) {
                dice = AttackResolver.extractDamageExpression(feature.getDescriptionRusloc());
            }
            Integer saveDc = feature.getSaveDc() != null ? feature.getSaveDc().intValue() : null;
            String saveAbilityCode = feature.getSaveAbility() != null ? feature.getSaveAbility().getCode() : null;
            boolean ranged = feature.getRangeFt() != null;
            Integer reachFt = feature.getReachFt() != null ? Integer.valueOf(feature.getReachFt()) : null;
            Integer rangeNormalFt = feature.getRangeFt() != null ? Integer.valueOf(feature.getRangeFt()) : null;
            Integer rangeLongFt = feature.getRangeLongFt() != null ? Integer.valueOf(feature.getRangeLongFt()) : rangeNormalFt;
            Integer rechargeMin = feature.getRechargeMin() != null ? Integer.valueOf(feature.getRechargeMin()) : null;
            return new AttackOption(feature.getNameRusloc(), bonus, dice, damageType, damageTypeId, saveDc, saveAbilityCode,
                    ranged, reachFt, rangeNormalFt, rangeLongFt, feature.getId(), rechargeMin);
        }

        throw new BadRequestException("This combatant cannot attack");
    }

    /** Target armor class: monster's authored AC or character's AC; falls back to 10. */
    private int resolveTargetAc(BattleCombatant target) {
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null
                && target.getMonster().getArmorClass() != null) {
            return target.getMonster().getArmorClass().intValue();
        }
        if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null
                && target.getCharacter().getArmorClass() != null) {
            // Static sheet AC plus any AC modifiers from active effects (Shield of Faith, Mage Armour…),
            // aggregated across both effect systems. Legacy buffs contribute nothing to AC today, so this
            // is additive with no change to existing behaviour.
            return target.getCharacter().getArmorClass()
                    + modifierAggregator.totalFor(target.getCharacter().getId(), ModifierTarget.ac());
        }
        return 10;
    }

    /**
     * Applies a signed HP {@code delta} to a combatant. For characters the change is written
     * through to the sheet (temp HP absorbs damage first, healing is capped at max HP) and an
     * {@code HP_CHANGED} event keeps the character views in sync; the combatant row mirrors the
     * result. Monsters are tracked solely on the combatant row.
     */
    private void applyDamageOrHeal(BattleCombatant combatant, int delta, User actor, UUID campaignId) {
        applyDamageOrHeal(combatant, delta, actor, campaignId, false);
    }

    /**
     * Применяет знаковую дельту HP к комбатанту (фаза 3.5 — с флагом {@code silent}). При {@code
     * silent=false} изменение логируется как DAMAGE/HEAL с «обратной дельтой» (undo_payload) — такую
     * запись можно откатить через {@code POST /undo}. При {@code silent=true} HP меняется без записи в
     * журнал (используется самим откатом, чтобы не создавать новую обратимую запись).
     *
     * @param combatant комбатант, чьё HP меняется
     * @param delta     знаковая дельта (отрицательная — урон, положительная — лечение)
     * @param actor     инициатор изменения
     * @param campaignId идентификатор кампании (для событий/лога)
     * @param silent    подавить запись в журнал (режим отката)
     */
    private void applyDamageOrHeal(BattleCombatant combatant, int delta, User actor, UUID campaignId, boolean silent) {
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null) {
            // Character HP goes through the single shared primitive (pessimistic lock, temp-HP
            // absorption, tracker mirroring and HP_CHANGED all live there). Mirror the authoritative
            // result back onto this combatant row so the action response reflects the change.
            int before = nz(combatant.getCurrentHp());
            HpChangeResult result = characterHpService.applyDelta(
                    combatant.getCharacter().getId(), delta, campaignId, actor.getId());
            combatant.setCurrentHp(result.currentHp());
            if (result.maxHp() > 0) {
                combatant.setMaxHp(result.maxHp());
            }
            combatantRepository.save(combatant);
            logHpChange(combatant, delta, campaignId, actor.getId(), silent);
            handleDeathSaveTransitions(combatant, before, result.currentHp(), delta, actor, campaignId);
            checkConcentrationOnDamage(combatant, delta, result.currentHp(), actor, campaignId);
        } else {
            int maxHp = combatant.getMaxHp() != null ? combatant.getMaxHp() : 0;
            int currentHp = combatant.getCurrentHp() != null ? combatant.getCurrentHp() : 0;
            if (delta < 0) {
                currentHp = Math.max(0, currentHp + delta);
            } else if (delta > 0) {
                currentHp = maxHp > 0 ? Math.min(maxHp, currentHp + delta) : currentHp + delta;
            }
            combatant.setCurrentHp(currentHp);
            combatantRepository.save(combatant);
            logHpChange(combatant, delta, campaignId, actor.getId(), silent);
        }
    }

    /**
     * Combat-log a HP delta as DAMAGE (delta&lt;0) or HEAL (delta&gt;0); no-op for a 0 delta or when
     * {@code silent}. Записывает «обратную дельту» (undo_payload {@code {kind:HP, combatantId, delta}}),
     * поэтому любое изменение HP становится обратимым через {@code POST /undo} (фаза 3.5).
     */
    private void logHpChange(BattleCombatant combatant, int delta, UUID campaignId, UUID actorUserId, boolean silent) {
        if (silent || delta == 0 || combatant.getBattle() == null) {
            return;
        }
        Map<String, Object> hpLog = new HashMap<>();
        hpLog.put("targetName", combatant.getDisplayName());
        hpLog.put("amount", Math.abs(delta));
        hpLog.put("newHp", combatant.getCurrentHp());
        if (combatant.getMaxHp() != null) {
            hpLog.put("maxHp", combatant.getMaxHp());
        }
        Map<String, Object> undo = new HashMap<>();
        undo.put("kind", "HP");
        undo.put("combatantId", combatant.getId().toString());
        undo.put("delta", delta);
        battleLogService.append(combatant.getBattle().getId(), campaignId,
                delta < 0 ? BattleLogType.DAMAGE : BattleLogType.HEAL,
                null, combatant.getId(), hpLog, BattleLogVisibility.PUBLIC, actorUserId, undo);
    }

    /**
     * Concentration side-effect of damage to a character combatant (Phase 2.2). Dropping to 0 HP ends
     * concentration outright; otherwise the server does NOT roll the save for the player — it records
     * the required Constitution save DC (= max(10, floor(damage/2))) as a PENDING check, and the player
     * (or GM) rolls it themselves via the concentration-check endpoint. If several damage instances
     * arrive before the player rolls, the highest DC is kept. No-op unless effects are on and the
     * character is actually concentrating.
     */
    private void checkConcentrationOnDamage(BattleCombatant combatant, int delta, int currentHp,
                                            User actor, UUID campaignId) {
        if (delta >= 0 || combatant.getCharacter() == null) {
            return;
        }
        UUID characterId = combatant.getCharacter().getId();
        if (!featureEffectService.isConcentrating(characterId)) {
            return;
        }
        int damage = -delta;
        if (currentHp <= 0) {
            // Dropping to 0 HP ends concentration with no save.
            featureEffectService.endConcentration(characterId);
            combatant.setPendingConcentrationDc(null);
            combatantRepository.save(combatant);
            logConcentration(combatant, campaignId, actor, "BROKEN", damage, null, null, null);
            return;
        }
        int dc = Math.max(10, damage / 2);
        int pending = combatant.getPendingConcentrationDc() == null
                ? dc : Math.max(combatant.getPendingConcentrationDc(), dc);
        combatant.setPendingConcentrationDc(pending);
        combatantRepository.save(combatant);
        logConcentration(combatant, campaignId, actor, "CHECK_REQUIRED", damage, pending, null, null);
    }

    /**
     * Выполняет операции "resolve concentration" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param d20 входящее значение d20, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleResponse resolveConcentration(UUID campaignId, UUID battleId, UUID combatantId,
                                               Integer d20, String username) {
        User user = getUser(username);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Concentration checks only happen in an active battle");
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));
        if (combatant.getBattle() == null || !combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);
        if (combatant.getCharacter() == null) {
            throw new BadRequestException("Only characters make concentration checks");
        }
        Integer dc = combatant.getPendingConcentrationDc();
        if (dc == null) {
            throw new BadRequestException("No pending concentration check for this combatant");
        }
        if (d20 != null && (d20 < 1 || d20 > 20)) {
            throw new BadRequestException("d20 must be between 1 and 20");
        }
        UUID characterId = combatant.getCharacter().getId();
        int roll = d20 != null ? d20 : diceRoller.rollD20();
        int saveBonus = resolveTargetSaveBonus(combatant, "con");
        AttackResolver.SaveOutcome save = AttackResolver.resolveSave(roll, saveBonus, dc);
        boolean broken = save == AttackResolver.SaveOutcome.FAIL;
        if (broken && featureEffectService.isConcentrating(characterId)) {
            featureEffectService.endConcentration(characterId);
        }
        combatant.setPendingConcentrationDc(null);
        combatantRepository.save(combatant);
        logConcentration(combatant, campaignId, user, broken ? "BROKEN" : "HELD",
                null, dc, roll + saveBonus, save.name());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    private void logConcentration(BattleCombatant combatant, UUID campaignId, User actor, String outcome,
                                  Integer damage, Integer dc, Integer saveTotal, String saveOutcome) {
        if (combatant.getBattle() == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetName", combatant.getDisplayName());
        payload.put("outcome", outcome);
        if (damage != null) {
            payload.put("damage", damage);
        }
        if (dc != null) {
            payload.put("dc", dc);
        }
        if (saveTotal != null) {
            payload.put("saveTotal", saveTotal);
        }
        if (saveOutcome != null) {
            payload.put("save", saveOutcome);
        }
        battleLogService.append(combatant.getBattle().getId(), campaignId, BattleLogType.CONCENTRATION,
                null, combatant.getId(), payload, BattleLogVisibility.PUBLIC, actor.getId());
    }

    /**
     * Death-save side effects of an HP change on a character combatant (1.3): dropping to 0 makes it
     * unconscious and resets the death-save counters; damage taken while already down is an automatic
     * failure (three ⇒ dead); healing above 0 clears the counters and the unconscious condition.
     */
    private void handleDeathSaveTransitions(BattleCombatant combatant, int before, int after, int delta,
                                            User actor, UUID campaignId) {
        PlayerCharacter character = combatant.getCharacter();
        int round = nz(combatant.getBattle() != null ? combatant.getBattle().getRoundNumber() : null);
        if (after <= 0) {
            if (before > 0) {
                character.setDeathSaveSuccesses(0);
                character.setDeathSaveFailures(0);
                characterRepository.save(character);
                conditionService.applyByCode(campaignId, combatant, "unconscious", actor.getId(), round);
                logDeathSaveEvent(combatant, campaignId, actor.getId(), "DOWNED", BattleLogVisibility.PUBLIC, null);
            } else if (delta < 0) {
                // Damage while already at 0 HP: automatic death-save failure (crit ⇒ 2 is a later refinement).
                int failures = Math.min(3, nz(character.getDeathSaveFailures()) + 1);
                character.setDeathSaveFailures(failures);
                if (failures >= 3) {
                    character.setStatus(CharacterStatus.DEAD);
                    characterRepository.save(character);
                    logDeathSaveEvent(combatant, campaignId, actor.getId(), "DEAD", BattleLogVisibility.PUBLIC, null);
                } else {
                    characterRepository.save(character);
                    // Pip counts are private (owner/GM only); the public "unconscious" state stays visible.
                    logDeathSaveEvent(combatant, campaignId, actor.getId(), "AUTO_FAIL",
                            BattleLogVisibility.GM_ONLY, Map.of("failures", failures));
                }
            }
        } else if (before <= 0) {
            character.setDeathSaveSuccesses(0);
            character.setDeathSaveFailures(0);
            character.setStatus(CharacterStatus.ACTIVE);
            characterRepository.save(character);
            conditionService.removeByCode(campaignId, combatant.getId(), "unconscious", actor.getId());
            logDeathSaveEvent(combatant, campaignId, actor.getId(), "REVIVED", BattleLogVisibility.PUBLIC, null);
        }
    }

    /** Combat-log a death-save transition; {@code extra} carries private detail (e.g. pip counts). */
    private void logDeathSaveEvent(BattleCombatant combatant, UUID campaignId, UUID actorUserId,
                                   String event, BattleLogVisibility visibility, Map<String, Object> extra) {
        if (combatant.getBattle() == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetName", combatant.getDisplayName());
        payload.put("event", event);
        if (extra != null) {
            payload.putAll(extra);
        }
        battleLogService.append(combatant.getBattle().getId(), campaignId, BattleLogType.DEATH_SAVE,
                null, combatant.getId(), payload, visibility, actorUserId);
    }

    // ================================ Helpers ================================

    /** The GM/admin may act for any combatant; a player only for their own active character. */
    private void enforceControls(UUID campaignId, User user, BattleCombatant combatant) {
        if (user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId())) {
            return;
        }
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null
                && combatant.getCharacter().getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the GM or the active character's owner can act for this combatant");
    }

    private void enforceCanEndTurn(UUID campaignId, User user, BattleCombatant current) {
        if (user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId())) {
            return;
        }
        if (current.getType() == CombatantType.CHARACTER && current.getCharacter() != null
                && current.getCharacter().getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the GM or the active character's owner can end this turn");
    }

    private UUID activeCombatantId(Battle battle, List<BattleCombatant> ordered) {
        if (battle.getStatus() != BattleStatus.ACTIVE || ordered.isEmpty()) {
            return null;
        }
        return ordered.get(clampIndex(battle.getCurrentTurnIndex(), ordered.size())).getId();
    }

    private int dexValue(PlayerCharacter character) {
        return character.getStats().stream()
                .filter(s -> s.getStatType() != null && DEX_CODE.equals(s.getStatType().getSlug()))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);
    }

    private int dexBuffBonus(PlayerCharacter character) {
        int total = 0;
        for (CharacterActiveEffect effect : character.getActiveEffects()) {
            BuffDebuff bd = effect.getBuffDebuff();
            if (bd != null && "STAT_MODIFIER".equals(bd.getEffectType())
                    && bd.getTargetStat() != null && DEX_CODE.equals(bd.getTargetStat().getSlug())
                    && bd.getModifierValue() != null) {
                total += Boolean.TRUE.equals(bd.getIsBuff()) ? bd.getModifierValue() : -bd.getModifierValue();
            }
        }
        // Feature-effect contributions to initiative / Dexterity (formula-evaluated); additive with the
        // legacy buffs summed above.
        total += modifierAggregator.featureTotal(character.getId(), ModifierTarget.initiative(DEX_CODE));
        return total;
    }

    private List<BattleCombatant> orderedCombatants(UUID battleId) {
        List<BattleCombatant> list = new ArrayList<>(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId));
        list.sort(Comparator
                .comparing(BattleCombatant::getTurnOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(c -> c.getCreatedAt() == null ? Instant.EPOCH : c.getCreatedAt()));
        return list;
    }

    private static int clampIndex(Integer index, int size) {
        int i = index == null ? 0 : index;
        if (i < 0) return 0;
        if (i >= size) return size - 1;
        return i;
    }

    private void requireStatus(Battle battle, BattleStatus expected, String message) {
        if (battle.getStatus() != expected) {
            throw new BadRequestException(message);
        }
    }

    private Battle findBattle(UUID battleId, UUID campaignId) {
        return battleRepository.findByIdAndCampaignId(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
    }

    /**
     * Row-locking battle load (SELECT ... FOR UPDATE) for every mutating combat action. It both
     * serializes concurrent mutations on the same battle and fixes the lock order (battle first,
     * then combatant/item rows) so the per-combatant locks below cannot deadlock.
     */
    private Battle findBattleForUpdate(UUID battleId, UUID campaignId) {
        return battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ====================== Map-service integration contracts ======================

    /**
     * Возвращает результат операции "get battle access" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public BattleAccessResponse getBattleAccess(UUID campaignId, UUID battleId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        campaignService.findCampaign(campaignId);
        findBattle(battleId, campaignId);

        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, userId);
        boolean canView = gm || campaignService.isMemberOfCampaign(campaignId, userId);

        List<UUID> controllableCombatantIds = new ArrayList<>();
        List<UUID> controllableCharacterIds = new ArrayList<>();
        if (canView) {
            for (BattleCombatant c : orderedCombatants(battleId)) {
                boolean owns = c.getType() == CombatantType.CHARACTER && c.getCharacter() != null
                        && c.getCharacter().getOwner() != null
                        && c.getCharacter().getOwner().getId().equals(userId);
                if (gm || owns) {
                    controllableCombatantIds.add(c.getId());
                    if (c.getType() == CombatantType.CHARACTER && c.getCharacter() != null) {
                        controllableCharacterIds.add(c.getCharacter().getId());
                    }
                }
            }
        }

        return BattleAccessResponse.builder()
                .battleId(battleId)
                .campaignId(campaignId)
                .userId(userId)
                .canView(canView)
                .canManageBattle(gm)
                .canControlAnyCombatant(gm)
                .controllableCombatantIds(controllableCombatantIds)
                .controllableCharacterIds(controllableCharacterIds)
                .build();
    }

    /**
     * Возвращает результат операции "get combatant reference" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CombatantReferenceResponse getCombatantReference(UUID campaignId, UUID battleId, UUID combatantId) {
        campaignService.findCampaign(campaignId);
        Battle battle = findBattle(battleId, campaignId);
        BattleCombatant c = combatantRepository.findById(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!c.getBattle().getId().equals(battleId)) {
            throw new ResourceNotFoundException("Combatant does not belong to this battle");
        }

        return CombatantReferenceResponse.builder()
                .battleId(battleId)
                .campaignId(campaignId)
                .combatantId(c.getId())
                .type(c.getType().name())
                .displayName(c.getDisplayName())
                .characterId(c.getCharacter() != null ? c.getCharacter().getId() : null)
                .monsterId(c.getMonster() != null ? c.getMonster().getId() : null)
                .ownerUserId(c.getCharacter() != null && c.getCharacter().getOwner() != null
                        ? c.getCharacter().getOwner().getId() : null)
                .currentHp(c.getCurrentHp())
                .maxHp(c.getMaxHp())
                .turnOrder(c.getTurnOrder())
                .currentTurn(toCombatantResponse(c, battle).isCurrentTurn())
                .widthCells(1)
                .heightCells(1)
                .build();
    }

    // ================================ Mapping ================================

    private BattleResponse toResponse(Battle battle, List<BattleCombatant> combatants) {
        List<BigDecimal> crValues = combatants.stream()
                .filter(c -> c.getType() == CombatantType.MONSTER && c.getMonster() != null)
                .map(c -> c.getMonster().getCrValue())
                .toList();
        List<Integer> xpBases = combatants.stream()
                .filter(c -> c.getType() == CombatantType.MONSTER && c.getMonster() != null)
                .map(c -> c.getMonster().getXpBase())
                .toList();
        int monsterCount = (int) combatants.stream().filter(c -> c.getType() == CombatantType.MONSTER).count();

        UUID currentCombatantId = null;
        if (battle.getStatus() == BattleStatus.ACTIVE && !combatants.isEmpty()) {
            currentCombatantId = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size())).getId();
        }
        final UUID activeId = currentCombatantId;

        List<BattleCombatantResponse> combatantResponses = combatants.stream()
                .map(c -> toCombatantResponse(c, battle, activeId))
                .toList();

        return BattleResponse.builder()
                .id(battle.getId())
                .campaignId(battle.getCampaign().getId())
                .name(battle.getName())
                .status(battle.getStatus().name())
                .roundNumber(battle.getRoundNumber())
                .currentTurnIndex(battle.getCurrentTurnIndex())
                .currentCombatantId(currentCombatantId)
                .monsterCount(monsterCount)
                .averageDanger(CombatCalculator.averageDanger(crValues))
                .totalXp(CombatCalculator.totalXp(xpBases, battle.getOverrideXp()))
                .overrideXp(battle.getOverrideXp())
                .combatants(combatantResponses)
                .startedAt(battle.getStartedAt())
                .endedAt(battle.getEndedAt())
                .createdAt(battle.getCreatedAt())
                .updatedAt(battle.getUpdatedAt())
                .build();
    }

    private BattleCombatantResponse toCombatantResponse(BattleCombatant c, Battle battle) {
        UUID activeId = null;
        if (battle.getStatus() == BattleStatus.ACTIVE && c.getTurnOrder() != null
                && c.getTurnOrder().equals(battle.getCurrentTurnIndex())) {
            activeId = c.getId();
        }
        return toCombatantResponse(c, battle, activeId);
    }

    private BattleCombatantResponse toCombatantResponse(BattleCombatant c, Battle battle, UUID activeCombatantId) {
        boolean currentTurn = battle.getStatus() == BattleStatus.ACTIVE
                && activeCombatantId != null && activeCombatantId.equals(c.getId());
        return BattleCombatantResponse.builder()
                .id(c.getId())
                .type(c.getType().name())
                .displayName(c.getDisplayName())
                .monsterId(c.getMonster() != null ? c.getMonster().getId() : null)
                .characterId(c.getCharacter() != null ? c.getCharacter().getId() : null)
                .ownerUserId(c.getCharacter() != null && c.getCharacter().getOwner() != null
                        ? c.getCharacter().getOwner().getId() : null)
                .instanceIndex(c.getInstanceIndex())
                .initiative(c.getInitiative())
                .initiativeRoll(c.getInitiativeRoll())
                .turnOrder(c.getTurnOrder())
                .currentHp(c.getCurrentHp())
                .maxHp(c.getMaxHp())
                .currentTurn(currentTurn)
                .actionMax(nz(c.getActionMax()))
                .actionSpent(nz(c.getActionSpent()))
                .bonusActionMax(nz(c.getBonusActionMax()))
                .bonusActionSpent(nz(c.getBonusActionSpent()))
                .legendaryActionMax(nz(c.getLegendaryActionMax()))
                .legendaryActionSpent(nz(c.getLegendaryActionSpent()))
                .reactionUsed(Boolean.TRUE.equals(c.getReactionUsed()))
                .conditions(conditionService.conditionsForCombatant(c.getId()))
                .deathSaveSuccesses(c.getCharacter() != null ? nz(c.getCharacter().getDeathSaveSuccesses()) : 0)
                .deathSaveFailures(c.getCharacter() != null ? nz(c.getCharacter().getDeathSaveFailures()) : 0)
                .dead(c.getCharacter() != null && c.getCharacter().getStatus() == CharacterStatus.DEAD)
                .concentrating(c.getCharacter() != null
                        && featureEffectService.isConcentrating(c.getCharacter().getId()))
                .pendingConcentrationDc(c.getPendingConcentrationDc())
                .dashing(Boolean.TRUE.equals(c.getDashing()))
                .dodging(Boolean.TRUE.equals(c.getDodging()))
                .disengaged(Boolean.TRUE.equals(c.getDisengaged()))
                .hidden(Boolean.TRUE.equals(c.getHidden()))
                .helpAdvantage(Boolean.TRUE.equals(c.getHelpAdvantage()))
                .legendaryResistanceMax(nz(c.getLegendaryResistanceMax()))
                .legendaryResistanceUsed(nz(c.getLegendaryResistanceUsed()))
                .attacksRemaining(c.getAttacksRemaining())
                .identityHidden(Boolean.TRUE.equals(c.getIdentityHidden()))
                .publicName(Boolean.TRUE.equals(c.getIdentityHidden())
                        ? "Неизвестное существо #" + nz(c.getInstanceIndex()) : null)
                .speedOverrideFt(c.getSpeedOverrideFt())
                .flying(Boolean.TRUE.equals(c.getFlying()))
                .hover(canHover(c))
                .surprised(Boolean.TRUE.equals(c.getSurprised()))
                .readiedAction(c.getReadiedAction())
                .build();
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }

    /** Clears a combatant's spent action economy (action / bonus / legendary) and movement budget at the start of their turn. */
    private void resetActionEconomy(BattleCombatant combatant) {
        combatant.setActionSpent(0);
        combatant.setBonusActionSpent(0);
        combatant.setLegendaryActionSpent(0);
        combatant.setReactionUsed(false);
        combatant.setMovementUsedFt(0);
        // Standard-action states that last "until the start of your next turn" clear now (Phase 2.7).
        // `hidden` is deliberately NOT reset here — it persists across turns until the combatant
        // attacks or is discovered.
        combatant.setDashing(false);
        combatant.setDodging(false);
        combatant.setDisengaged(false);
        combatant.setHelpAdvantage(false);
        // Неиспользованное подготовленное действие (Ready, 3.7) истекает в начале следующего хода.
        combatant.setReadiedAction(null);
        // Multiattack budget for the turn (Phase 2.9): a monster with Multiattack may make N attacks
        // with its Attack action; others keep the single-attack (action-based) guard (null budget).
        if (combatant.getType() == CombatantType.MONSTER && combatant.getMonster() != null) {
            int multi = parseMultiattack(combatant.getMonster());
            combatant.setAttacksRemaining(multi > 0 ? multi : null);
        }
        combatantRepository.save(combatant);
    }

    // ============================== Movement budget ============================

    /**
     * Выполняет операции "apply movement" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MovementResultResponse applyMovement(UUID campaignId, UUID battleId, MovementRequest request) {
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(request.getCombatantId())
                .filter(c -> c.getBattle().getId().equals(battleId))
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));

        int speedFt = resolveSpeedFt(combatant, request.getMode());
        int used = nz(combatant.getMovementUsedFt());
        int feet = Math.max(0, nz(request.getFeet()));

        if (battle.getStatus() != BattleStatus.ACTIVE) {
            return movementResult(false, "BATTLE_NOT_ACTIVE", speedFt, used, false, request.isGmOverride());
        }

        List<BattleCombatant> ordered = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        boolean activeTurn = combatant.getId().equals(activeCombatantId(battle, ordered));
        boolean withinBudget = used + feet <= speedFt;

        boolean allowed;
        String reason = null;
        if (request.isGmOverride()) {
            allowed = true;
        } else if (!activeTurn) {
            allowed = false;
            reason = "NOT_ACTIVE_TURN";
        } else if (!withinBudget) {
            allowed = false;
            reason = "MOVEMENT_BUDGET_EXCEEDED";
        } else {
            allowed = true;
        }

        if (allowed) {
            used += feet;
            combatant.setMovementUsedFt(used);
            combatantRepository.save(combatant);
            if (request.isGmOverride()) {
                log.info("Movement GM override: battleId={}, combatantId={}, feet={}, used={}/{}",
                        battleId, combatant.getId(), feet, used, speedFt);
            }
        }
        return movementResult(allowed, reason, speedFt, used, withinBudget, request.isGmOverride());
    }

    private MovementResultResponse movementResult(boolean allowed, String reason, int speedFt, int usedFt,
                                                  boolean withinBudget, boolean gmOverride) {
        return MovementResultResponse.builder()
                .allowed(allowed)
                .reason(reason)
                .remainingFt(speedFt - usedFt)
                .speedFt(speedFt)
                .withinBudget(withinBudget)
                .gmOverride(gmOverride)
                .build();
    }

    /**
     * Выполняет операции "movement context" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MovementContextResponse movementContext(UUID campaignId, UUID battleId) {
        Battle battle = findBattle(battleId, campaignId);
        List<BattleCombatant> ordered = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, ordered);
        List<MovementContextResponse.CombatantMovement> entries = new ArrayList<>();
        for (BattleCombatant c : ordered) {
            int speed = resolveSpeedFt(c);
            int usedFt = nz(c.getMovementUsedFt());
            entries.add(MovementContextResponse.CombatantMovement.builder()
                    .combatantId(c.getId())
                    .speedFt(speed)
                    .movementUsedFt(usedFt)
                    .remainingFt(speed - usedFt)
                    .build());
        }
        return MovementContextResponse.builder()
                .activeCombatantId(activeId)
                .roundNumber(battle.getRoundNumber())
                .combatants(entries)
                .build();
    }

    /**
     * Бюджет перемещения комбатанта на этот ход в футах для режима ходьбы (совместимость): делегирует
     * mode-aware версии с {@code null}-режимом.
     *
     * @param combatant комбатант, чей бюджет считаем
     * @return бюджет перемещения в футах
     */
    private int resolveSpeedFt(BattleCombatant combatant) {
        return resolveSpeedFt(combatant, null);
    }

    /**
     * Бюджет перемещения комбатанта на этот ход в футах для указанного режима движения (фаза 2.11):
     * базовая скорость соответствующего режима с учётом GM-override, удвоенная при Dash (фаза 2.7).
     *
     * @param combatant комбатант, чей бюджет считаем
     * @param mode      режим движения (WALK/FLY/SWIM/CLIMB/BURROW; {@code null} → ходьба)
     * @return бюджет перемещения в футах
     */
    private int resolveSpeedFt(BattleCombatant combatant, String mode) {
        int base = baseSpeedFt(combatant, mode);
        return Boolean.TRUE.equals(combatant.getDashing()) ? base * 2 : base;
    }

    /**
     * Базовая скорость ходьбы комбатанта в футах без учёта Dash (совместимость): делегирует mode-aware
     * версии с режимом ходьбы.
     *
     * @param combatant комбатант, чью базовую скорость определяем
     * @return базовая скорость ходьбы в футах
     */
    private int baseSpeedFt(BattleCombatant combatant) {
        return baseSpeedFt(combatant, null);
    }

    /**
     * Базовая скорость перемещения комбатанта в футах для режима движения, без учёта Dash (фаза 2.11).
     * Приоритет — ручной GM-override; иначе для персонажа берётся его единственная скорость с листа, для
     * монстра — скорость нужного режима из статблока, а при её отсутствии — скорость ходьбы (по умолчанию
     * {@link #DEFAULT_SPEED_FT}).
     *
     * @param combatant комбатант, чью скорость определяем
     * @param mode      режим движения (WALK/FLY/SWIM/CLIMB/BURROW; {@code null} → ходьба)
     * @return базовая скорость в футах (неотрицательная)
     */
    private int baseSpeedFt(BattleCombatant combatant, String mode) {
        if (combatant.getSpeedOverrideFt() != null && combatant.getSpeedOverrideFt() >= 0) {
            return combatant.getSpeedOverrideFt();
        }
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null) {
            Integer speed = combatant.getCharacter().getSpeed();
            return speed != null && speed > 0 ? speed : DEFAULT_SPEED_FT;
        }
        if (combatant.getType() == CombatantType.MONSTER && combatant.getMonster() != null
                && combatant.getMonster().getSpeeds() != null) {
            String code = movementModeCode(mode);
            Integer byMode = combatant.getMonster().getSpeeds().stream()
                    .filter(ms -> ms.getMovementType() != null && code.equals(ms.getMovementType().getCode()))
                    .map(MonsterSpeed::getFt)
                    .filter(ft -> ft != null && ft > 0)
                    .findFirst()
                    .orElse(null);
            if (byMode != null) {
                return byMode;
            }
            return combatant.getMonster().getSpeeds().stream()
                    .filter(ms -> ms.getMovementType() != null && WALK_CODE.equals(ms.getMovementType().getCode()))
                    .map(MonsterSpeed::getFt)
                    .filter(ft -> ft != null && ft > 0)
                    .findFirst()
                    .orElse(DEFAULT_SPEED_FT);
        }
        return DEFAULT_SPEED_FT;
    }

    /**
     * Нормализует режим движения из запроса в код скорости статблока монстра.
     *
     * @param mode режим движения (WALK/FLY/SWIM/CLIMB/BURROW; {@code null} → ходьба)
     * @return код типа перемещения статблока ({@code walk}/{@code fly}/{@code swim}/{@code climb}/{@code burrow})
     */
    private static String movementModeCode(String mode) {
        if (mode == null) {
            return WALK_CODE;
        }
        return switch (mode.toUpperCase()) {
            case "FLY" -> "fly";
            case "SWIM" -> "swim";
            case "CLIMB" -> "climb";
            case "BURROW" -> "burrow";
            default -> WALK_CODE;
        };
    }
}
