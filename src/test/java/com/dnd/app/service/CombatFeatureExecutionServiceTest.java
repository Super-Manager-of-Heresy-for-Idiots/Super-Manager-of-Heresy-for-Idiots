package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatFeatureExecutionServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private CharacterFeatureResolver resolver;
    @Mock private ClassFeatureRepository classFeatureRepository;
    @Mock private FeatureDamageRuleRepository damageRepository;
    @Mock private FeatureHealingRuleRepository healingRepository;
    @Mock private FeatureResolutionRuleRepository resolutionRepository;
    @Mock private FeatureAttackRuleRepository attackRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private CharacterFormulaContextFactory contextFactory;
    @Mock private CharacterHpService hpService;
    @Mock private ModifierAggregator modifierAggregator;
    @Mock private FeatureUseLogRepository useLogRepository;

    @InjectMocks private CombatFeatureExecutionService service;

    private final UUID featureId = UUID.randomUUID();

    @Test
    void planComputesDamageDiceFromFormula() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(classFeatureRepository.findById(featureId))
                .thenReturn(Optional.of(ClassFeature.builder().id(featureId).title("Fireball").build()));
        FeatureRule rule = FeatureRule.builder().id(UUID.randomUUID()).build();
        when(resolver.approvedEnabledRules(anyList())).thenReturn(List.of(rule));
        when(contextFactory.build(any())).thenReturn(org.mockito.Mockito.mock(FormulaContext.class));

        UUID diceFormulaId = UUID.randomUUID();
        when(damageRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(
                FeatureDamageRule.builder().id(UUID.randomUUID()).featureRuleId(rule.getId())
                        .diceFormulaId(diceFormulaId).requiresSave(true).halfOnSave(true).build()));
        when(healingRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());
        when(resolutionRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());
        when(attackRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());

        FeatureFormula formula = FeatureFormula.builder().id(diceFormulaId).expression("dice(\"8d6\")").build();
        when(formulaRepository.findById(diceFormulaId)).thenReturn(Optional.of(formula));
        when(formulaService.evaluateDice(any(), any())).thenReturn(new DiceValue(8, 6));

        FeatureExecutionPlan plan = service.plan(PlayerCharacter.builder().id(UUID.randomUUID()).build(), featureId);

        assertThat(plan.getDamages()).hasSize(1);
        assertThat(plan.getDamages().get(0).getDiceExpression()).isEqualTo("8d6");
        assertThat(plan.getDamages().get(0).isRequiresSave()).isTrue();
    }

    @Test
    void planIsEmptyWhenRuntimeDisabled() {
        when(classFeatureRepository.findById(featureId))
                .thenReturn(Optional.of(ClassFeature.builder().id(featureId).title("X").build()));
        when(flags.isRuntimeEnabled()).thenReturn(false);
        lenient().when(resolver.approvedEnabledRules(anyList())).thenReturn(List.of());

        FeatureExecutionPlan plan = service.plan(PlayerCharacter.builder().id(UUID.randomUUID()).build(), featureId);
        assertThat(plan.getDamages()).isEmpty();
        assertThat(plan.isRequiresManualAdjudication()).isFalse();
    }

    @Test
    void planRequiresManualAdjudicationWhenNoApprovedRules() {
        when(classFeatureRepository.findById(featureId))
                .thenReturn(Optional.of(ClassFeature.builder().id(featureId).title("Indomitable").build()));
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(resolver.approvedEnabledRules(anyList())).thenReturn(List.of());

        FeatureExecutionPlan plan = service.plan(PlayerCharacter.builder().id(UUID.randomUUID()).build(), featureId);

        assertThat(plan.getFeatureId()).isEqualTo(featureId);
        assertThat(plan.getFeatureName()).isEqualTo("Indomitable");
        assertThat(plan.isRequiresManualAdjudication()).isTrue();
        assertThat(plan.getDamages()).isEmpty();
    }

    @Test
    void applyToTargetRoutesDamageThroughHpService() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(useLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UUID campaignId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        PlayerCharacter actor = PlayerCharacter.builder().id(UUID.randomUUID()).build();
        PlayerCharacter target = PlayerCharacter.builder().id(targetId).currentHp(10).maxHp(20).build();

        // Damage must flow through the shared HP primitive (temp HP, lock, tracker sync, HP_CHANGED),
        // not be written to current_hp here. The service maps the primitive's result into the DTO.
        when(hpService.applyDelta(targetId, -5, campaignId, actorUserId))
                .thenReturn(new HpChangeResult(targetId, 5, 0, 20, false));

        FeatureApplyResult result =
                service.applyToTarget(actor, featureId, target, 5, 0, null, campaignId, actorUserId);

        assertThat(result.getDamageApplied()).isEqualTo(5);
        assertThat(result.getTargetCurrentHp()).isEqualTo(5);
        assertThat(result.getTargetMaxHp()).isEqualTo(20);
        verify(hpService).applyDelta(targetId, -5, campaignId, actorUserId);
    }

    @Test
    void planForSpellFeedsSlotLevelIntoFormulasAndUsesSpellRules() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        UUID spellId = UUID.randomUUID();
        com.dnd.app.domain.Spell spell = com.dnd.app.domain.Spell.builder()
                .id(spellId).slug("fireball").nameRu("Огненный шар").level(3).build();
        FeatureRule rule = FeatureRule.builder().id(UUID.randomUUID()).build();
        when(resolver.approvedEnabledRules(
                org.mockito.ArgumentMatchers.eq(com.dnd.app.domain.featurerule.FeatureRuleOwnerType.SPELL),
                anyList()))
                .thenReturn(List.of(rule));
        // The character snapshot itself knows no slot level — the overlay must supply it.
        FormulaContext base = org.mockito.Mockito.mock(FormulaContext.class);
        when(contextFactory.build(any())).thenReturn(base);

        UUID dcFormulaId = UUID.randomUUID();
        when(damageRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());
        when(healingRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());
        when(resolutionRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(
                com.dnd.app.domain.featurerule.FeatureResolutionRule.builder()
                        .id(UUID.randomUUID()).featureRuleId(rule.getId())
                        .resolutionType("saving_throw").dcFormulaId(dcFormulaId).build()));
        when(attackRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of());
        when(formulaRepository.findById(dcFormulaId)).thenReturn(Optional.of(
                FeatureFormula.builder().id(dcFormulaId).expression("8 + proficiency_bonus").build()));

        org.mockito.ArgumentCaptor<FormulaContext> ctx =
                org.mockito.ArgumentCaptor.forClass(FormulaContext.class);
        when(formulaService.evaluateInteger(any(FeatureFormula.class), ctx.capture())).thenReturn(15);

        FeatureExecutionPlan plan = service.planForSpell(
                PlayerCharacter.builder().id(UUID.randomUUID()).build(), spell, 3);

        assertThat(plan.getFeatureName()).isEqualTo("Огненный шар");
        assertThat(plan.getResolutions()).hasSize(1);
        assertThat(plan.getResolutions().get(0).getDc()).isEqualTo(15);
        assertThat(ctx.getValue().scalar("spell_slot_level")).isEqualTo(3.0);
    }

    @Test
    void applySpellToTargetLogsSpellWithoutClassFeatureId() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        UUID spellId = UUID.randomUUID();
        com.dnd.app.domain.Spell spell = com.dnd.app.domain.Spell.builder()
                .id(spellId).slug("fireball").nameRu("Огненный шар").level(3).build();
        FeatureRule spellRule = FeatureRule.builder().id(UUID.randomUUID()).build();
        when(resolver.approvedEnabledRules(
                org.mockito.ArgumentMatchers.eq(com.dnd.app.domain.featurerule.FeatureRuleOwnerType.SPELL),
                anyList()))
                .thenReturn(List.of(spellRule));

        UUID targetId = UUID.randomUUID();
        PlayerCharacter target = PlayerCharacter.builder().id(targetId).currentHp(20).maxHp(20).build();
        when(hpService.applyDelta(eq(targetId), eq(-7), any(), any()))
                .thenReturn(new HpChangeResult(targetId, 13, 0, 20, false));

        org.mockito.ArgumentCaptor<com.dnd.app.domain.featurerule.FeatureUseLog> log =
                org.mockito.ArgumentCaptor.forClass(com.dnd.app.domain.featurerule.FeatureUseLog.class);
        when(useLogRepository.save(log.capture())).thenAnswer(i -> i.getArgument(0));

        FeatureApplyResult result = service.applySpellToTarget(
                PlayerCharacter.builder().id(UUID.randomUUID()).build(), spell, target,
                7, 0, null, UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.getDamageApplied()).isEqualTo(7);
        // feature_use_log.feature_id has an FK to class_feature — a spell must never be written there.
        assertThat(log.getValue().getFeatureId()).isNull();
        assertThat(log.getValue().getFeatureRuleId()).isEqualTo(spellRule.getId());
        assertThat(log.getValue().getActionType()).isEqualTo("spell_resolution");
    }

    @Test
    void applyToTargetHalvesResistedDamage() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(useLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UUID campaignId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID fireTypeId = UUID.randomUUID();
        PlayerCharacter actor = PlayerCharacter.builder().id(UUID.randomUUID()).build();
        PlayerCharacter target = PlayerCharacter.builder().id(targetId).currentHp(20).maxHp(20).build();

        when(modifierAggregator.damageMultiplier(targetId, fireTypeId)).thenReturn(0.5);
        when(hpService.applyDelta(targetId, -4, campaignId, actorUserId))
                .thenReturn(new HpChangeResult(targetId, 16, 0, 20, false));

        // 9 fire damage against resistance → floor(9 * 0.5) = 4 actually applied.
        FeatureApplyResult result =
                service.applyToTarget(actor, featureId, target, 9, 0, fireTypeId, campaignId, actorUserId);

        assertThat(result.getDamageApplied()).isEqualTo(4);
        verify(hpService).applyDelta(targetId, -4, campaignId, actorUserId);
    }
}
