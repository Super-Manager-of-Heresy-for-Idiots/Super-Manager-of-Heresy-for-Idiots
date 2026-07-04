package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureAttackRule;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureAttackRuleRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.DiceValue;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Structured combat resolution for a feature (Stage 8): computes what to roll (damage dice, DC, save/attack
 * requirements) from the feature's rules + the actor's context, and applies an already-rolled outcome to a
 * target character's HP. Actual dice rolls happen at the client/GM. Deep integration into the core
 * BattleService flow is intentionally deferred; this stays additive and flag-gated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombatFeatureExecutionService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureDamageRuleRepository damageRepository;
    private final FeatureHealingRuleRepository healingRepository;
    private final FeatureResolutionRuleRepository resolutionRepository;
    private final FeatureAttackRuleRepository attackRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final PlayerCharacterRepository characterRepository;
    private final FeatureUseLogRepository useLogRepository;

    @Transactional(readOnly = true)
    public FeatureExecutionPlan plan(PlayerCharacter actor, UUID featureId) {
        ClassFeature feature = classFeatureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено: " + featureId));

        FeatureExecutionPlan.FeatureExecutionPlanBuilder plan = FeatureExecutionPlan.builder()
                .featureId(featureId).featureName(feature.getTitle())
                .damages(List.of()).healings(List.of()).resolutions(List.of()).attacks(List.of())
                .requiresManualAdjudication(false);

        List<FeatureRule> rules = resolver.approvedEnabledRules(List.of(featureId));
        if (!flags.isRuntimeEnabled() || rules.isEmpty()) {
            return plan.build();
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        FormulaContext ctx = contextFactory.build(actor);

        boolean manual = rules.stream()
                .anyMatch(r -> FeatureRuleProfile.MANUAL_ADJUDICATION.getCode().equals(r.getRuleType()));

        List<FeatureExecutionPlan.Damage> damages = damageRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(dr -> toDamage(dr, ctx)).toList();
        List<FeatureExecutionPlan.Healing> healings = healingRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(hr -> FeatureExecutionPlan.Healing.builder()
                        .amount(evalInt(hr.getAmountFormulaId(), ctx))
                        .tempHp(hr.isTempHp())
                        .canReviveFromZero(hr.isCanReviveFromZero())
                        .build()).toList();
        List<FeatureExecutionPlan.Resolution> resolutions = resolutionRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(rr -> FeatureExecutionPlan.Resolution.builder()
                        .resolutionType(rr.getResolutionType())
                        .abilityId(rr.getAbilityId()).skillId(rr.getSkillId())
                        .dc(evalInt(rr.getDcFormulaId(), ctx))
                        .build()).toList();
        List<FeatureExecutionPlan.Attack> attacks = attackRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(ar -> FeatureExecutionPlan.Attack.builder()
                        .attackKind(ar.getAttackKind())
                        .extraAttackCount(evalInt(ar.getExtraAttackCountFormulaId(), ctx))
                        .build()).toList();

        return plan.requiresManualAdjudication(manual)
                .damages(damages).healings(healings).resolutions(resolutions).attacks(attacks)
                .build();
    }

    /** Apply an already-rolled damage/healing outcome to a target character's HP and log it. */
    @Transactional
    public FeatureApplyResult applyToTarget(PlayerCharacter actor, UUID featureId, PlayerCharacter target,
                                            Integer damage, Integer healing) {
        if (!flags.isRuntimeEnabled()) {
            throw new BadRequestException("Runtime умений отключён");
        }
        int dmg = damage != null ? Math.max(0, damage) : 0;
        int heal = healing != null ? Math.max(0, healing) : 0;

        int current = target.getCurrentHp() != null ? target.getCurrentHp() : 0;
        int after = current - dmg + heal;
        if (after < 0) {
            after = 0;
        }
        if (target.getMaxHp() != null && after > target.getMaxHp()) {
            after = target.getMaxHp();
        }
        target.setCurrentHp(after);
        characterRepository.save(target);

        useLogRepository.save(FeatureUseLog.builder()
                .characterId(actor.getId())
                .featureId(featureId)
                .actionType("combat_resolution")
                .detail("dmg=" + dmg + ", heal=" + heal + ", target=" + target.getId() + ", hp=" + after)
                .build());

        return FeatureApplyResult.builder()
                .targetCharacterId(target.getId())
                .damageApplied(dmg).healingApplied(heal)
                .targetCurrentHp(after).targetMaxHp(target.getMaxHp())
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private FeatureExecutionPlan.Damage toDamage(FeatureDamageRule dr, FormulaContext ctx) {
        Integer saveDc = null;
        if (dr.getSaveRuleId() != null) {
            FeatureResolutionRule save = resolutionRepository.findById(dr.getSaveRuleId()).orElse(null);
            if (save != null) {
                saveDc = evalInt(save.getDcFormulaId(), ctx);
            }
        }
        return FeatureExecutionPlan.Damage.builder()
                .diceExpression(evalDice(dr.getDiceFormulaId(), ctx))
                .flatAmount(evalInt(dr.getFlatAmountFormulaId(), ctx))
                .damageTypeId(dr.getDamageTypeId())
                .requiresAttackHit(dr.isRequiresAttackHit())
                .requiresSave(dr.isRequiresSave())
                .halfOnSave(dr.isHalfOnSave())
                .saveDc(saveDc)
                .build();
    }

    private Integer evalInt(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Combat formula (int) failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }

    private String evalDice(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            DiceValue dice = formulaService.evaluateDice(formula, ctx);
            return dice.toExpression();
        } catch (FormulaException e) {
            log.warn("Combat formula (dice) failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }
}
