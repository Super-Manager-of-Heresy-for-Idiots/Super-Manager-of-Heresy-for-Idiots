package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.dto.combat.HpChangeResult;
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
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.DiceValue;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import com.dnd.app.service.formula.ScalarOverlayContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс CombatFeatureExecutionService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
    private final CharacterHpService hpService;
    private final ModifierAggregator modifierAggregator;
    private final FeatureUseLogRepository useLogRepository;

    /**
     * Выполняет операции "plan" в рамках бизнес-логики домена.
     * @param actor входящее значение actor, используемое бизнес-сценарием
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureExecutionPlan plan(PlayerCharacter actor, UUID featureId) {
        ClassFeature feature = classFeatureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено: " + featureId));
        List<FeatureRule> rules = resolver.approvedEnabledRules(List.of(featureId));
        return planForRules(actor, featureId, feature.getTitle(), rules, null);
    }

    /**
     * Выполняет операции "plan for spell" в рамках бизнес-логики домена.
     * @param actor входящее значение actor, используемое бизнес-сценарием
     * @param spell входящее значение spell, используемое бизнес-сценарием
     * @param slotLevel входящее значение slot level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureExecutionPlan planForSpell(PlayerCharacter actor, Spell spell, Integer slotLevel) {
        List<FeatureRule> rules =
                resolver.approvedEnabledRules(FeatureRuleOwnerType.SPELL, List.of(spell.getId()));
        FeatureExecutionPlan plan = planForRules(actor, spell.getId(), spell.getNameRu(), rules, slotLevel);
        // AoE template + lingering zone (Phase 2.3): static content data from the spell's structured
        // columns (091) — per-actor numbers stay rule-driven above; this is not a second damage path.
        if (spell.getAreaShape() != null) {
            plan.setArea(FeatureExecutionPlan.Area.builder()
                    .shape(spell.getAreaShape())
                    .sizeFt(spell.getAreaSizeFt())
                    .build());
        }
        if (Boolean.TRUE.equals(spell.getZonePersists())) {
            plan.setZone(FeatureExecutionPlan.Zone.builder()
                    .terrain(spell.getZoneTerrain())
                    .obscurement(spell.getZoneObscurement())
                    .persists(true)
                    .build());
        }
        return plan;
    }

    private FeatureExecutionPlan planForRules(PlayerCharacter actor, UUID ownerId, String displayName,
                                              List<FeatureRule> rules, Integer spellSlotLevel) {
        FeatureExecutionPlan.FeatureExecutionPlanBuilder plan = FeatureExecutionPlan.builder()
                .featureId(ownerId).featureName(displayName)
                .damages(List.of()).healings(List.of()).resolutions(List.of()).attacks(List.of())
                .requiresManualAdjudication(false);

        if (!flags.isRuntimeEnabled() || rules.isEmpty()) {
            return plan.build();
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        FormulaContext ctx = contextFactory.build(actor);
        if (spellSlotLevel != null) {
            ctx = new ScalarOverlayContext(ctx).scalar("spell_slot_level", spellSlotLevel);
        }
        final FormulaContext evalCtx = ctx;

        boolean manual = rules.stream()
                .anyMatch(r -> FeatureRuleProfile.MANUAL_ADJUDICATION.getCode().equals(r.getRuleType()));

        List<FeatureExecutionPlan.Damage> damages = damageRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(dr -> toDamage(dr, evalCtx)).toList();
        List<FeatureExecutionPlan.Healing> healings = healingRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(hr -> FeatureExecutionPlan.Healing.builder()
                        .amount(evalInt(hr.getAmountFormulaId(), evalCtx))
                        .tempHp(hr.isTempHp())
                        .canReviveFromZero(hr.isCanReviveFromZero())
                        .build()).toList();
        List<FeatureExecutionPlan.Resolution> resolutions = resolutionRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(rr -> FeatureExecutionPlan.Resolution.builder()
                        .resolutionType(rr.getResolutionType())
                        .abilityId(rr.getAbilityId()).skillId(rr.getSkillId())
                        .dc(evalInt(rr.getDcFormulaId(), evalCtx))
                        .build()).toList();
        List<FeatureExecutionPlan.Attack> attacks = attackRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .map(ar -> FeatureExecutionPlan.Attack.builder()
                        .attackKind(ar.getAttackKind())
                        .extraAttackCount(evalInt(ar.getExtraAttackCountFormulaId(), evalCtx))
                        .build()).toList();

        return plan.requiresManualAdjudication(manual)
                .damages(damages).healings(healings).resolutions(resolutions).attacks(attacks)
                .build();
    }

    /**
     * Выполняет операции "apply to target" в рамках бизнес-логики домена.
     * @param actor входящее значение actor, используемое бизнес-сценарием
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @param target входящее значение target, используемое бизнес-сценарием
     * @param damage входящее значение damage, используемое бизнес-сценарием
     * @param healing входящее значение healing, используемое бизнес-сценарием
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureApplyResult applyToTarget(PlayerCharacter actor, UUID featureId, PlayerCharacter target,
                                            Integer damage, Integer healing, UUID damageTypeId,
                                            UUID campaignId, UUID actorUserId) {
        FeatureApplyResult result = applyOutcome(target, damage, healing, damageTypeId, campaignId, actorUserId);
        useLogRepository.save(FeatureUseLog.builder()
                .characterId(actor.getId())
                .featureId(featureId)
                .actionType("combat_resolution")
                .detail("dmg=" + result.getDamageApplied() + ", heal=" + result.getHealingApplied()
                        + ", target=" + target.getId() + ", hp=" + result.getTargetCurrentHp())
                .build());
        return result;
    }

    /**
     * Выполняет операции "apply spell to target" в рамках бизнес-логики домена.
     * @param actor входящее значение actor, используемое бизнес-сценарием
     * @param spell входящее значение spell, используемое бизнес-сценарием
     * @param target входящее значение target, используемое бизнес-сценарием
     * @param damage входящее значение damage, используемое бизнес-сценарием
     * @param healing входящее значение healing, используемое бизнес-сценарием
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureApplyResult applySpellToTarget(PlayerCharacter actor, Spell spell, PlayerCharacter target,
                                                 Integer damage, Integer healing, UUID damageTypeId,
                                                 UUID campaignId, UUID actorUserId) {
        FeatureApplyResult result = applyOutcome(target, damage, healing, damageTypeId, campaignId, actorUserId);
        UUID spellRuleId = resolver.approvedEnabledRules(FeatureRuleOwnerType.SPELL, List.of(spell.getId())).stream()
                .map(FeatureRule::getId)
                .findFirst().orElse(null);
        useLogRepository.save(FeatureUseLog.builder()
                .characterId(actor.getId())
                .featureRuleId(spellRuleId)
                .actionType("spell_resolution")
                .detail("spell=" + spell.getSlug() + ", dmg=" + result.getDamageApplied()
                        + ", heal=" + result.getHealingApplied()
                        + ", target=" + target.getId() + ", hp=" + result.getTargetCurrentHp())
                .build());
        return result;
    }

    private FeatureApplyResult applyOutcome(PlayerCharacter target, Integer damage, Integer healing,
                                            UUID damageTypeId, UUID campaignId, UUID actorUserId) {
        if (!flags.isRuntimeEnabled()) {
            throw new BadRequestException("Runtime умений отключён");
        }
        int rolledDmg = damage != null ? Math.max(0, damage) : 0;
        int heal = healing != null ? Math.max(0, healing) : 0;

        // Structured damage carries a damage type from the shared reference table that resistances/
        // vulnerabilities key on — so the target's resist (÷2) / vulnerability (×2) applies cleanly here,
        // unlike the legacy attack path.
        int appliedDmg = applyResistance(target.getId(), rolledDmg, damageTypeId);

        HpChangeResult result = null;
        if (appliedDmg > 0) {
            result = hpService.applyDelta(target.getId(), -appliedDmg, campaignId, actorUserId);
        }
        if (heal > 0) {
            result = hpService.applyDelta(target.getId(), heal, campaignId, actorUserId);
        }

        int after = result != null ? result.currentHp()
                : (target.getCurrentHp() != null ? target.getCurrentHp() : 0);
        Integer maxHp = result != null && result.maxHp() > 0 ? result.maxHp() : target.getMaxHp();

        return FeatureApplyResult.builder()
                .targetCharacterId(target.getId())
                .damageApplied(appliedDmg).healingApplied(heal)
                .targetCurrentHp(after).targetMaxHp(maxHp)
                .build();
    }

    /** Halve on resistance / double on vulnerability (floored), or unchanged when the type is unknown. */
    private int applyResistance(UUID targetCharacterId, int damage, UUID damageTypeId) {
        if (damage <= 0 || damageTypeId == null) {
            return Math.max(0, damage);
        }
        double multiplier = modifierAggregator.damageMultiplier(targetCharacterId, damageTypeId);
        return (int) Math.floor(damage * multiplier);
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
