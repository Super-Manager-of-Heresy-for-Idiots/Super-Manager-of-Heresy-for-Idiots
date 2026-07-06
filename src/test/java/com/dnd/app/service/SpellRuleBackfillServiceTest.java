package com.dnd.app.service;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.SpellDamage;
import com.dnd.app.domain.content.SpellHealing;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.DurationUnit;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureAttackRule;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.dto.featurerule.SpellRuleBackfillResult;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.DurationUnitRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureAttackRuleRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpellRuleBackfillServiceTest {

    @Mock private SpellRepository spellRepository;
    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureRuleIssueRepository issueRepository;
    @Mock private FeatureRuleRevisionService revisionService;
    @Mock private FeatureFormulaService formulaService;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureDamageRuleRepository damageRuleRepository;
    @Mock private FeatureHealingRuleRepository healingRuleRepository;
    @Mock private FeatureResolutionRuleRepository resolutionRuleRepository;
    @Mock private FeatureAttackRuleRepository attackRuleRepository;
    @Mock private FeatureActionCostRepository actionCostRepository;
    @Mock private FeatureEffectDefinitionRepository effectDefinitionRepository;
    @Mock private FeatureEffectModifierRepository effectModifierRepository;
    @Mock private ActionTypeRepository actionTypeRepository;
    @Mock private DurationUnitRepository durationUnitRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private ContentSkillRepository skillRepository;

    @InjectMocks private SpellRuleBackfillService service;

    private final UUID spellId = UUID.randomUUID();
    private final UUID dexId = UUID.randomUUID();
    private final UUID strId = UUID.randomUUID();
    private final UUID fireTypeId = UUID.randomUUID();
    private final UUID saveResolutionId = UUID.randomUUID();

    private Spell fullSpell() {
        return Spell.builder()
                .id(spellId).slug("test-spell").nameRu("Тестовое заклинание").level(3)
                .saveAbility("DEXTERITY")
                .attackRoll(false)
                .castingActionSlug("bonus-action")
                .concentration(true)
                .durationType("time").durationAmount(1).durationUnit("minute")
                .damages(new java.util.ArrayList<>(List.of(
                        SpellDamage.builder().dice("8d6")
                                .damageType(DamageType.builder().id(fireTypeId).build()).build(),
                        SpellDamage.builder().dice(null).raw("непарсибельно").build())))
                .healings(new java.util.ArrayList<>(List.of(
                        SpellHealing.builder().dice("2d4").flat(2).build())))
                .linkedBuffs(Set.of(BuffDebuff.builder()
                        .id(UUID.randomUUID()).name("Благословение")
                        .effectType("STAT_MODIFIER")
                        .targetStat(StatType.builder().id(strId).slug("str").build())
                        .modifierValue(2).isBuff(true)
                        .build()))
                .build();
    }

    @BeforeEach
    void stubCommon() {
        lenient().when(statTypeRepository.findByHomebrewIsNull()).thenReturn(List.of(
                StatType.builder().id(strId).slug("str").build(),
                StatType.builder().id(dexId).slug("dex").build()));
        lenient().when(skillRepository.findAll()).thenReturn(List.of());
        lenient().when(ruleRepository.save(any())).thenAnswer(i -> {
            FeatureRule r = i.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        lenient().when(formulaService.validateAndStamp(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(formulaRepository.save(any())).thenAnswer(i -> {
            FeatureFormula f = i.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        lenient().when(resolutionRuleRepository.save(any())).thenAnswer(i -> {
            FeatureResolutionRule r = i.getArgument(0);
            r.setId(saveResolutionId);
            return r;
        });
        lenient().when(effectDefinitionRepository.save(any())).thenAnswer(i -> {
            FeatureEffectDefinition d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        lenient().when(actionTypeRepository.findByCode("bonus_action"))
                .thenReturn(java.util.Optional.of(ActionType.builder().id(UUID.randomUUID()).code("bonus_action").build()));
        lenient().when(durationUnitRepository.findByCode("minute"))
                .thenReturn(java.util.Optional.of(DurationUnit.builder().id(UUID.randomUUID()).build()));
    }

    @Test
    void backfillCreatesRulesForEveryAspectAndReconciles() {
        when(spellRepository.findAll()).thenReturn(List.of(fullSpell()));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(List.of());

        SpellRuleBackfillResult result = service.backfill(true);

        // Reconciliation invariant of every pass: sourceSpells == rulesCreated + skippedExisting.
        assertThat(result.getDamage().getSourceSpells()).isEqualTo(1);
        assertThat(result.getDamage().getRulesCreated()).isEqualTo(1);
        assertThat(result.getDamage().getSourceRows()).isEqualTo(2);
        assertThat(result.getDamage().getSkippedNoData()).isEqualTo(1); // the dice-less row
        assertThat(result.getHealing().getRulesCreated()).isEqualTo(1);
        assertThat(result.getResolution().getRulesCreated()).isEqualTo(1);
        assertThat(result.getActionCost().getRulesCreated()).isEqualTo(1);
        assertThat(result.getEffects().getRulesCreated()).isEqualTo(1);

        // The damage row links the save gate created by the resolution pass of the same run.
        ArgumentCaptor<FeatureDamageRule> dmg = ArgumentCaptor.forClass(FeatureDamageRule.class);
        verify(damageRuleRepository).save(dmg.capture());
        assertThat(dmg.getValue().getDamageTypeId()).isEqualTo(fireTypeId);
        assertThat(dmg.getValue().isRequiresSave()).isTrue();
        assertThat(dmg.getValue().getSaveRuleId()).isEqualTo(saveResolutionId);

        // The save uses the RAW spell-DC formula and the DEX ability id.
        ArgumentCaptor<FeatureResolutionRule> res = ArgumentCaptor.forClass(FeatureResolutionRule.class);
        verify(resolutionRuleRepository).save(res.capture());
        assertThat(res.getValue().getResolutionType()).isEqualTo("saving_throw");
        assertThat(res.getValue().getAbilityId()).isEqualTo(dexId);
        ArgumentCaptor<FeatureFormula> formulas = ArgumentCaptor.forClass(FeatureFormula.class);
        verify(formulaRepository, atLeastOnce()).save(formulas.capture());
        assertThat(formulas.getAllValues())
                .anyMatch(f -> SpellRuleBackfillService.SPELL_DC_EXPRESSION.equals(f.getExpression()));
        // dice+flat healing collapses into one integer formula
        assertThat(formulas.getAllValues()).anyMatch(f -> "2d4 + 2".equals(f.getExpression()));

        // Healing row, action cost row, effect definition + stat modifier all created.
        verify(healingRuleRepository).save(any(FeatureHealingRule.class));
        verify(actionCostRepository).save(any(FeatureActionCost.class));
        ArgumentCaptor<FeatureEffectDefinition> def = ArgumentCaptor.forClass(FeatureEffectDefinition.class);
        verify(effectDefinitionRepository).save(def.capture());
        assertThat(def.getValue().getEffectKey()).isEqualTo("buff:Благословение");
        assertThat(def.getValue().isConcentrationRequired()).isTrue();
        ArgumentCaptor<FeatureEffectModifier> mod = ArgumentCaptor.forClass(FeatureEffectModifier.class);
        verify(effectModifierRepository).save(mod.capture());
        assertThat(mod.getValue().getModifierType()).isEqualTo("stat_bonus");
        assertThat(mod.getValue().getAbilityId()).isEqualTo(strId);

        // No attack roll on this spell → no attack rule; the unknown-on-save issue is recorded once.
        verify(attackRuleRepository, never()).save(any(FeatureAttackRule.class));
        assertThat(result.getIssuesCreated()).isEqualTo(1);
        verify(issueRepository).save(any());

        // Every created rule is versioned and auto-approved (deterministic structured source).
        verify(revisionService, times(5)).createInitialDraft(any(), anyString());
        verify(revisionService, times(5)).approveCurrent(any(), anyString(), anyString());
    }

    @Test
    void dryRunReportsSameCountsButPersistsNothing() {
        when(spellRepository.findAll()).thenReturn(List.of(fullSpell()));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(List.of());

        SpellRuleBackfillResult result = service.backfill(false);

        assertThat(result.getDamage().getRulesCreated()).isEqualTo(1);
        assertThat(result.getHealing().getRulesCreated()).isEqualTo(1);
        assertThat(result.getResolution().getRulesCreated()).isEqualTo(1);
        assertThat(result.getActionCost().getRulesCreated()).isEqualTo(1);
        assertThat(result.getEffects().getRulesCreated()).isEqualTo(1);
        verify(ruleRepository, never()).save(any());
        verify(formulaRepository, never()).save(any());
        verify(revisionService, never()).createInitialDraft(any(), anyString());
    }

    @Test
    void backfillSkipsAspectsThatAlreadyHaveMigrationRules() {
        when(spellRepository.findAll()).thenReturn(List.of(fullSpell()));
        List<FeatureRule> existing = List.of(
                migrationRule("save_check_attack"), migrationRule("damage"), migrationRule("healing"),
                migrationRule("action_cost"), migrationRule("active_effect"));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(existing);
        when(resolutionRuleRepository.findByFeatureRuleId(any())).thenReturn(List.of());

        SpellRuleBackfillResult result = service.backfill(true);

        assertThat(result.getDamage().getSkippedExisting()).isEqualTo(1);
        assertThat(result.getHealing().getSkippedExisting()).isEqualTo(1);
        assertThat(result.getResolution().getSkippedExisting()).isEqualTo(1);
        assertThat(result.getActionCost().getSkippedExisting()).isEqualTo(1);
        assertThat(result.getEffects().getSkippedExisting()).isEqualTo(1);
        assertThat(result.getDamage().getRulesCreated()).isZero();
        verify(ruleRepository, never()).save(any());
        verify(revisionService, never()).approveCurrent(any(), anyString(), anyString());
    }

    @Test
    void laterSaveRuleUpgradesExistingDamageRuleGate() {
        // Run 1 created only a damage rule (the save was unparsed back then); the admin has since
        // resolved the warning, so run 2 creates the save rule and must link the old damage row to it.
        Spell spell = fullSpell();
        FeatureRule existingDamage = migrationRule("damage");
        when(spellRepository.findAll()).thenReturn(List.of(spell));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(List.of(existingDamage));
        FeatureDamageRule orphanRow = FeatureDamageRule.builder()
                .id(UUID.randomUUID()).featureRuleId(existingDamage.getId()).requiresSave(false).build();
        when(damageRuleRepository.findByFeatureRuleId(existingDamage.getId())).thenReturn(List.of(orphanRow));

        service.backfill(true);

        ArgumentCaptor<FeatureDamageRule> updated = ArgumentCaptor.forClass(FeatureDamageRule.class);
        verify(damageRuleRepository).save(updated.capture());
        assertThat(updated.getValue().getId()).isEqualTo(orphanRow.getId());
        assertThat(updated.getValue().isRequiresSave()).isTrue();
        assertThat(updated.getValue().getSaveRuleId()).isEqualTo(saveResolutionId);
    }

    private FeatureRule migrationRule(String ruleType) {
        return FeatureRule.builder()
                .id(UUID.randomUUID()).ownerType("SPELL").ownerId(spellId)
                .ruleType(ruleType).source(FeatureRuleSource.MIGRATION.getCode())
                .build();
    }
}
