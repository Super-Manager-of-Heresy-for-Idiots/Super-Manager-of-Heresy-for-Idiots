package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Casting a KNOWN spell through the feature-rules runtime (S2 spell-stack absorption). This service is
 * deliberately only the cost glue — slot, combat action economy, effects, audit — around the shared
 * execution engine: the structured resolution itself (damage dice, DC, healing) is computed by
 * {@link CombatFeatureExecutionService#planForSpell} and outcomes are applied by
 * {@link CombatFeatureExecutionService#applySpellToTarget}, exactly as for class features. There is no
 * separate spell resolution engine.
 *
 * <p>Costs are spent in rollback-safe order: combat action first (throws when the turn slot is already
 * used), then the spell slot — a failure anywhere rolls back the whole cast. Gated by
 * {@code app.feature-rules.spells} (with the master runtime switch). Not covered here yet, by design:
 * ritual casting without a slot, concentration enforcement, and the free-cast path of feature spell
 * grants (Stage 9's {@code castViaFeature}).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellCastService {

    private final FeatureRulesProperties flags;
    private final SpellRepository spellRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final CharacterFeatureResolver resolver;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final CombatActionEconomyService economyService;
    private final SpellSlotService slotService;
    private final CombatFeatureExecutionService executionService;
    private final FeatureEffectService effectService;
    private final GameplayEventService gameplayEventService;
    private final FeatureUseLogRepository useLogRepository;

    /**
     * Cast a spell the character knows: spend the combat action (if in combat) and the slot, compute the
     * roll plan, apply the spell's active effects to {@code effectTarget} (already access-checked by the
     * controller; the caster when null), publish a {@code spell_cast} gameplay event and log the use.
     */
    @Transactional
    public SpellCastResult cast(PlayerCharacter caster, UUID spellId, SpellCastRequest request,
                                PlayerCharacter effectTarget) {
        if (!flags.spellsActive()) {
            throw new BadRequestException("Runtime заклинаний отключён");
        }
        Spell spell = requireSpell(spellId);
        if (!knownSpellRepository.existsByCharacterIdAndSpellId(caster.getId(), spellId)) {
            throw new BadRequestException("Персонаж не знает это заклинание");
        }
        List<FeatureRule> rules =
                resolver.approvedEnabledRules(FeatureRuleOwnerType.SPELL, List.of(spellId));

        // 1. Combat action economy FIRST: if the declared slot is already spent this turn, the whole
        //    cast (including the spell slot below) rolls back untouched.
        UUID combatId = request != null ? request.getCombatId() : null;
        String actionCode = actionCode(rules);
        if (combatId != null && actionCode != null) {
            economyService.spend(combatId, caster.getId(), actionCode);
        }

        // 2. Spell slot (cantrips are free). Upcasting: any slot of the spell's level or higher.
        Integer slotLevel = null;
        SpellSlotsResponse slots = null;
        int spellLevel = spell.getLevel() != null ? spell.getLevel() : 0;
        if (spellLevel > 0) {
            slotLevel = request != null && request.getSlotLevel() != null
                    ? request.getSlotLevel() : spellLevel;
            if (slotLevel < spellLevel) {
                throw new BadRequestException(
                        "Ячейка " + slotLevel + "-го уровня ниже уровня заклинания (" + spellLevel + ")");
            }
            slots = slotService.expendInternal(caster.getId(), slotLevel);
        }

        // 3. The shared engine computes what to roll; the slot level feeds spell_slot_level formulas.
        FeatureExecutionPlan plan = executionService.planForSpell(caster, spell, slotLevel);

        // 4. Active effects (backfilled from spell_buffs) land on the chosen target, caster by default.
        PlayerCharacter target = effectTarget != null ? effectTarget : caster;
        int effectsApplied = effectService.applyForSpellCast(caster, target, spellId);

        gameplayEventService.publish(caster, "spell_cast", combatId,
                "{\"spellId\":\"" + spellId + "\",\"slotLevel\":" + slotLevel + "}");
        useLogRepository.save(FeatureUseLog.builder()
                .characterId(caster.getId())
                .featureRuleId(rules.stream().map(FeatureRule::getId).findFirst().orElse(null))
                .combatId(combatId)
                .actionType(actionCode != null ? actionCode : "spell_cast")
                .detail("spell=" + spell.getSlug() + (slotLevel != null ? ", slot=L" + slotLevel : ""))
                .build());

        return SpellCastResult.builder()
                .spellId(spellId)
                .spellName(spell.getNameRu())
                .slotLevelUsed(slotLevel)
                .actionType(combatId != null ? actionCode : null)
                .effectsApplied(effectsApplied)
                .plan(plan)
                .slots(slots)
                .message("Заклинание использовано")
                .build();
    }

    /** Apply an already-rolled spell outcome to a target — the shared HP primitive, spell-attributed log. */
    @Transactional
    public FeatureApplyResult applyToTarget(
            PlayerCharacter caster, UUID spellId, PlayerCharacter target,
            Integer damage, Integer healing, UUID damageTypeId, UUID campaignId, UUID actorUserId) {
        Spell spell = requireSpell(spellId);
        return executionService.applySpellToTarget(
                caster, spell, target, damage, healing, damageTypeId, campaignId, actorUserId);
    }

    /** Roll plan for a known spell without spending anything (preview; upcast level optional). */
    @Transactional(readOnly = true)
    public FeatureExecutionPlan plan(PlayerCharacter caster, UUID spellId, Integer slotLevel) {
        Spell spell = requireSpell(spellId);
        int spellLevel = spell.getLevel() != null ? spell.getLevel() : 0;
        Integer effective = spellLevel > 0
                ? (slotLevel != null ? slotLevel : spellLevel)
                : null;
        return executionService.planForSpell(caster, spell, effective);
    }

    /**
     * The action-economy code of the spell's approved {@code action_cost} rule. Null when the spell has
     * no approved cost data — the runtime then follows the FR philosophy that unapproved rules have no
     * gameplay effect (no cost is guessed).
     */
    private String actionCode(List<FeatureRule> rules) {
        if (rules.isEmpty()) {
            return null;
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        return actionCostRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .findFirst()
                .map(FeatureActionCost::getActionTypeId)
                .flatMap(actionTypeRepository::findById)
                .map(ActionType::getCode)
                .orElse(null);
    }

    private Spell requireSpell(UUID spellId) {
        return spellRepository.findById(spellId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено: " + spellId));
    }
}
