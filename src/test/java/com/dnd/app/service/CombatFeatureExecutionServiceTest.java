package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
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
import static org.mockito.Mockito.lenient;
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
    @Mock private PlayerCharacterRepository characterRepository;
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
    void applyToTargetReducesHpByDamageAndClamps() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(useLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PlayerCharacter actor = PlayerCharacter.builder().id(UUID.randomUUID()).build();
        PlayerCharacter target = PlayerCharacter.builder().id(UUID.randomUUID()).currentHp(10).maxHp(20).build();

        FeatureApplyResult result = service.applyToTarget(actor, featureId, target, 5, 0);
        assertThat(result.getTargetCurrentHp()).isEqualTo(5);
        assertThat(target.getCurrentHp()).isEqualTo(5);

        // Overkill clamps to 0
        FeatureApplyResult result2 = service.applyToTarget(actor, featureId, target, 999, 0);
        assertThat(result2.getTargetCurrentHp()).isZero();
    }
}
