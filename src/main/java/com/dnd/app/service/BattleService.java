package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
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
import com.dnd.app.dto.request.BulkActionRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.InitiativeOrderRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.MovementRequest;
import com.dnd.app.dto.request.SpendActionRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.domain.StatType;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.AttackResolver;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.CombatCalculator;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
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
 * Lifecycle and turn logic of a campaign battle. The GM assembles a monster group (preview of
 * average danger / total xp), starts the fight (rolling initiative for every monster), players
 * join their characters with a d20, and the shared initiative tracker drives turn passing.
 * Every state change is broadcast to the campaign topic so all participants stay in sync; the
 * client re-fetches the authoritative {@link BattleResponse} on each ping.
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
    private final StatTypeRepository statTypeRepository;
    private final FeatureEffectService featureEffectService;
    private final com.dnd.app.integration.map.MapZoneCreator mapZoneCreator;

    // ================================ Lifecycle ================================

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

    @Transactional(readOnly = true)
    public List<BattleResponse> listBattles(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return battleRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(b -> toResponse(b, combatantRepository.findByBattleIdOrderByTurnOrderAsc(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BattleResponse getBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * Combat log for a battle (Phase 1.2), seq-ordered after {@code afterSeq}. Non-GM callers never
     * receive GM_ONLY rows (e.g. private death-save pips) — the filter is applied server-side.
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

    /** Apply a condition to a combatant. GM applies to anyone; a player only to their own character (like HP delta). */
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
        battleLogService.append(battleId, campaignId, BattleLogType.CONDITION, null, combatant.getId(),
                condLog, BattleLogVisibility.PUBLIC, user.getId());
        return result;
    }

    /** Remove a condition from a combatant (same permission rules as applying one). */
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
     * Mass GM operation over several combatants at once (Phase 2.4): damage/heal a flat amount, or
     * add/remove a condition. Reuses the SAME per-target primitives as the single-target flows
     * ({@code applyDamageOrHeal} + save/mitigation, {@link ConditionService}) — no copied logic — in a
     * single transaction, with one summary GM log entry and one battle-updated broadcast. GM/admin only.
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
     * Roll a death saving throw for a dying (0 HP) character (server d20 or a manual result). nat20 →
     * back up at 1 HP; nat1 → two failures; 10+ → a success (three ⇒ stable); below 10 → a failure
     * (three ⇒ dead). Permission as for HP: the GM or the character's owner (1.3).
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

    /** Stabilize a dying character (GM/healer, Medicine done by hand): clear the death-save counters; stays unconscious. */
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
     * Reroll one combatant's initiative (GM quick tool, Phase 1.7): the server rolls a d20, recomputes
     * initiative (d20 + Dexterity/statblock bonus), re-sorts the whole tracker and keeps the turn
     * anchored on whoever is currently acting. Logs it and broadcasts a turn change. GM/admin only.
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
     * Replace the whole tracker's initiative values in one shot (GM quick tool, Phase 1.7): the
     * request must list every combatant of the battle exactly once. Each initiative is set as a
     * manual GM value (roll cleared), the tracker is re-sorted and the turn stays anchored on
     * whoever is currently acting. GM/admin only. Broadcasts a turn change and logs it.
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
     * Cast a spell inside the battle flow (Phase 2.1). Validates it is the caster's turn and that the
     * caller controls them, then delegates to the SINGLE cast path — {@link SpellCastService#cast}
     * (which spends the action via the unified economy with {@code combatId=battleId}, spends the
     * slot, runs the shared feature-rules engine and publishes {@code spell_cast}) — and records a
     * {@code SPELL} battle-log entry. Returns the cast result (incl. the execution plan: dice / DC /
     * save ability). Gated by {@code app.feature-rules.*}: throws when the runtime is disabled.
     *
     * <p>Effects apply to a character effect-target (self/ally); spell DAMAGE to monster combatants
     * (the plan → resolve → {@code applyDamageOrHeal} bridge reusing 0.4/0.5) is a follow-up (2.1b).
     * Monster spellcasters are out of scope here.
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

    /** Total damage dealt to the spell target + the resistance modifier applied (for the cast result). */
    private record SpellDamageSummary(int total, String modifier) {
        boolean applied() {
            return total > 0;
        }
        static SpellDamageSummary none() {
            return new SpellDamageSummary(0, null);
        }
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
                        damages.get(i), target, saveAbilityCode, manualForLine, user, campaignId, battleId);
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
            String saveAbilityCode, Integer manualDamage, User user, UUID campaignId, UUID battleId) {
        // Attack-roll spells need a spell-attack bonus the plan does not carry → GM adjudicates.
        if (dmg.isRequiresAttackHit()) {
            logSpellManual(battleId, campaignId, target, "attack_roll", user);
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
                logSpellManual(battleId, campaignId, target, "unresolved_save", user);
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

    private void logSpellManual(UUID battleId, UUID campaignId, BattleCombatant target, String reason, User user) {
        battleLogService.append(battleId, campaignId, BattleLogType.SPELL, null, target.getId(),
                Map.of("event", "SPELL_DAMAGE_MANUAL", "reason", reason), BattleLogVisibility.GM_ONLY, user.getId());
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
     * The fixed part of a character's initiative — Dexterity modifier plus the net of active
     * Dexterity buffs/debuffs — so the client can preview the final initiative live as the
     * player enters or rolls a d20 ({@code initiative = d20 + bonus}). Same math the join uses.
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

    @Transactional
    public BattleResponse endTurn(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        // Row-lock the battle so two simultaneous end-turn calls can't both advance it.
        Battle battle = battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
        requireStatus(battle, BattleStatus.ACTIVE, "Turns can only be passed in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }

        int currentIndex = clampIndex(battle.getCurrentTurnIndex(), combatants.size());
        BattleCombatant current = combatants.get(currentIndex);
        enforceCanEndTurn(campaignId, user, current);

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
     * The combatant whose turn it currently is strikes a target. The attacker supplies their own
     * d20 (tabletop style); the server resolves hit/crit against the target's AC, rolls the named
     * attack's damage and applies it to the target's HP. When the target is a character the change
     * is written through to its sheet (with temp-HP absorption) so death/HP persists after the
     * battle. Authorization: the GM (any combatant, e.g. monsters) or the owner of the active
     * character, and only on that combatant's own turn.
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
        BattleCombatant attacker = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, attacker);

        // Lock the attacker row and reject a second action this turn: one action per turn is modelled,
        // multi-attack is not, so a duplicate/racing attack must not spend the action again.
        attacker = combatantRepository.findByIdForUpdate(attacker.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attacker combatant not found"));
        if (attacker.getActionSpent() >= attacker.getActionMax()) {
            throw new BadRequestException("The action has already been used this turn");
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

        if (attack.saveDc() != null) {
            // Save-based attack (e.g. a monster's breath weapon): the TARGET rolls a saving throw with
            // its own bonus (ability modifier + proficiency/statblock save + active effects) against the
            // DC — the attacker makes no attack roll. Success halves the damage, failure takes it full.
            rollMode = request.getSaveRollMode() != null ? request.getSaveRollMode() : AttackRollMode.NORMAL;
            roll = resolveRoll("save", rollMode, request.getSaveD20(), request.getSaveD20A(), request.getSaveD20B());
            effectiveD20 = roll.effectiveD20();
            int saveBonus = resolveTargetSaveBonus(target, attack.saveAbilityCode());
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
            rollMode = request.getRollMode() != null ? request.getRollMode() : AttackRollMode.NORMAL;
            roll = resolveAttackRoll(request);
            effectiveD20 = roll.effectiveD20();
            targetAc = resolveTargetAc(target);
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
        battleLogService.append(battleId, campaignId, BattleLogType.ATTACK, attacker.getId(), target.getId(),
                attackLog, BattleLogVisibility.PUBLIC, user.getId());

        if (damage != null && damage > 0) {
            applyDamageOrHeal(target, -damage, user, campaignId);
        }

        attacker.setActionSpent(attacker.getActionSpent() + 1);
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
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
    }

    /**
     * The active character consumes a carried item (e.g. drinks a healing potion). Only the
     * combatant whose turn it is may act, and only with an item they own. The item's healing dice
     * (the consumable template's damage dice, read as restoration) are rolled and applied to the
     * chosen target — by default the user themselves — then one unit of the item is spent.
     */
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
     * Declaratively marks one of a combatant's action-economy slots (action / bonus action /
     * reaction) as spent for this turn. Actions and bonus actions may only be spent on the
     * combatant's own turn; a reaction can be spent at any time. Re-spending a slot is rejected.
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

    /**
     * GM manual HP adjustment of any combatant (negative {@code delta} damages, positive heals).
     * Bookkeeping for NPCs and corrections outside the attack flow; writes through to the sheet
     * for characters.
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
     * GM adjustment of a combatant's action-economy maxima (action / bonus action / legendary
     * action). Models the pools growing with level or spells and grants legendary actions. Only the
     * provided fields change; spent counters are clamped so they never exceed the new maximum.
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
                                UUID damageTypeId, Integer saveDc, String saveAbilityCode) {
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
                    .map(a -> new AttackOption(a.getName(),
                            AttackResolver.parseAttackBonus(a.getAttackBonus()),
                            a.getDamage(), a.getDamageType(), null, null, null))
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
            return new AttackOption(feature.getNameRusloc(), bonus, dice, damageType, damageTypeId, saveDc, saveAbilityCode);
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
            logHpChange(combatant, delta, campaignId, actor.getId());
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
            logHpChange(combatant, delta, campaignId, actor.getId());
        }
    }

    /** Combat-log a HP delta as DAMAGE (delta&lt;0) or HEAL (delta&gt;0); no-op for a 0 delta. */
    private void logHpChange(BattleCombatant combatant, int delta, UUID campaignId, UUID actorUserId) {
        if (delta == 0 || combatant.getBattle() == null) {
            return;
        }
        Map<String, Object> hpLog = new HashMap<>();
        hpLog.put("targetName", combatant.getDisplayName());
        hpLog.put("amount", Math.abs(delta));
        hpLog.put("newHp", combatant.getCurrentHp());
        if (combatant.getMaxHp() != null) {
            hpLog.put("maxHp", combatant.getMaxHp());
        }
        battleLogService.append(combatant.getBattle().getId(), campaignId,
                delta < 0 ? BattleLogType.DAMAGE : BattleLogType.HEAL,
                null, combatant.getId(), hpLog, BattleLogVisibility.PUBLIC, actorUserId);
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
     * Resolve a pending concentration save the PLAYER (or GM) rolls (Phase 2.2): a manual d20 or, when
     * {@code d20} is null, a server AUTO roll. A failure ends the character's concentration; either way
     * the pending check is cleared. Authorization: the character's owner or the GM.
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
     * Read-only projection of what a given user may do in a battle, for service-to-service callers
     * (map-service) that must authorize token control without touching the core DB or duplicating
     * combat permission rules. GM/admin manage the battle and control any combatant; a player can
     * only control their own character combatants; a non-member sees {@code canView=false}.
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
     * Minimal, safe identity of one combatant for map-service to create a token-combat link from.
     * Deliberately omits private character-sheet data; token footprint defaults to 1x1 because
     * spatial sizing is owned by map-service, not core BE.
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
        combatantRepository.save(combatant);
    }

    // ============================== Movement budget ============================

    /**
     * Internal (map-service) contract: validate and commit a combatant's movement spend for the
     * current turn. The spatial cost ({@code feet}) is computed authoritatively in map-service; the
     * per-turn budget lives here. Returns an allowed-flag envelope (never a 4xx for a legal "no")
     * carrying the reason and remaining budget so the caller can react precisely.
     *
     * <p>Concurrency: locks the battle row first (serializing all mutations on this battle), then the
     * combatant row — the established battle→combatant lock order — so two simultaneous moves cannot
     * drive the budget negative.
     */
    @Transactional
    public MovementResultResponse applyMovement(UUID campaignId, UUID battleId, MovementRequest request) {
        Battle battle = findBattleForUpdate(battleId, campaignId);
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(request.getCombatantId())
                .filter(c -> c.getBattle().getId().equals(battleId))
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found in battle"));

        int speedFt = resolveSpeedFt(combatant);
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
     * Read-only movement snapshot (active combatant + every combatant's speed/spent) so the tactical
     * UI (via map) can preview remaining movement without recomputing speed from the character sheet.
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

    /** A combatant's walking speed this turn, in feet: the character sheet speed, or the monster's walk speed. */
    private int resolveSpeedFt(BattleCombatant combatant) {
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null) {
            Integer speed = combatant.getCharacter().getSpeed();
            return speed != null && speed > 0 ? speed : DEFAULT_SPEED_FT;
        }
        if (combatant.getType() == CombatantType.MONSTER && combatant.getMonster() != null
                && combatant.getMonster().getSpeeds() != null) {
            return combatant.getMonster().getSpeeds().stream()
                    .filter(ms -> ms.getMovementType() != null && WALK_CODE.equals(ms.getMovementType().getCode()))
                    .map(MonsterSpeed::getFt)
                    .filter(ft -> ft != null && ft > 0)
                    .findFirst()
                    .orElse(DEFAULT_SPEED_FT);
        }
        return DEFAULT_SPEED_FT;
    }
}
