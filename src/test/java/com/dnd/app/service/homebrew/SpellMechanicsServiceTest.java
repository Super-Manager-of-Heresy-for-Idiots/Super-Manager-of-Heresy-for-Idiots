package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureRuleRevisionRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.service.FeatureFormulaService;
import com.dnd.app.service.FeatureRuleRevisionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест SpellMechanicsServiceTest фиксирует трансляцию GM-ввода в SPELL-owned feature_rules (P2-1 Phase B):
 * урон+спасбросок+лечение → три approved-правила (save_check_attack / damage / healing) с корректными связями.
 */
@ExtendWith(MockitoExtension.class)
class SpellMechanicsServiceTest {

    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureDamageRuleRepository damageRuleRepository;
    @Mock private FeatureHealingRuleRepository healingRuleRepository;
    @Mock private FeatureResolutionRuleRepository resolutionRuleRepository;
    @Mock private FeatureRuleRevisionRepository revisionRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private FeatureRuleRevisionService revisionService;
    @Mock private DamageTypeRepository damageTypeRepository;
    @Mock private StatTypeRepository statTypeRepository;

    @InjectMocks private SpellMechanicsService service;

    @Test
    @DisplayName("sync: урон 8d6 fire + DEX save (half) + лечение 2d8 → 3 approved SPELL-правила со связкой save→damage")
    void sync_damageSaveHealing_buildsThreeApprovedRules() {
        UUID spellId = UUID.randomUUID();
        Spell spell = new Spell();
        spell.setId(spellId);
        HomebrewPackage pkg = HomebrewPackage.builder().title("Pkg").build();
        pkg.setId(UUID.randomUUID());
        spell.setHomebrew(pkg);

        StatType dex = new StatType();
        dex.setId(UUID.randomUUID());
        dex.setSlug("dex");
        DamageType fire = new DamageType();
        fire.setId(UUID.randomUUID());
        fire.setSlug("fire");

        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(List.of());
        when(ruleRepository.save(any(FeatureRule.class))).thenAnswer(inv -> {
            FeatureRule r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(formulaService.validateAndStamp(any(FeatureFormula.class))).thenAnswer(inv -> {
            FeatureFormula f = inv.getArgument(0);
            f.setValidationStatus("valid");
            return f;
        });
        when(formulaRepository.save(any(FeatureFormula.class))).thenAnswer(inv -> {
            FeatureFormula f = inv.getArgument(0);
            if (f.getId() == null) f.setId(UUID.randomUUID());
            return f;
        });
        when(statTypeRepository.findByHomebrewIsNull()).thenReturn(List.of(dex));
        when(damageTypeRepository.findBySlugAndHomebrewIsNull("fire")).thenReturn(Optional.of(fire));
        when(resolutionRuleRepository.save(any(FeatureResolutionRule.class))).thenAnswer(inv -> {
            FeatureResolutionRule r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(damageRuleRepository.save(any(FeatureDamageRule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(healingRuleRepository.save(any(FeatureHealingRule.class))).thenAnswer(inv -> inv.getArgument(0));

        HomebrewSpellRequest req = new HomebrewSpellRequest();
        req.setDamageDice("8d6");
        req.setDamageType("fire");
        req.setSaveAbility("dex");
        req.setHalfOnSave(true);
        req.setHealingFormula("2d8");

        service.sync(spell, pkg, req, "gm");

        // 3 правила: save_check_attack, damage, healing
        ArgumentCaptor<FeatureRule> ruleCaptor = ArgumentCaptor.forClass(FeatureRule.class);
        verify(ruleRepository, times(3)).save(ruleCaptor.capture());
        assertTrue(ruleCaptor.getAllValues().stream().allMatch(r -> "SPELL".equals(r.getOwnerType())));
        assertTrue(ruleCaptor.getAllValues().stream().anyMatch(r -> "damage".equals(r.getRuleType())));
        assertTrue(ruleCaptor.getAllValues().stream().anyMatch(r -> "healing".equals(r.getRuleType())));
        assertTrue(ruleCaptor.getAllValues().stream().anyMatch(r -> "save_check_attack".equals(r.getRuleType())));

        // каждое правило approved
        verify(revisionService, times(3)).approveCurrent(any(), any(), eq("gm"));

        // damage связан с типом урона, спасбросоком и half_on_save
        ArgumentCaptor<FeatureDamageRule> dmgCaptor = ArgumentCaptor.forClass(FeatureDamageRule.class);
        verify(damageRuleRepository).save(dmgCaptor.capture());
        FeatureDamageRule dr = dmgCaptor.getValue();
        assertEquals(fire.getId(), dr.getDamageTypeId());
        assertTrue(dr.isRequiresSave());
        assertTrue(dr.isHalfOnSave());
        assertNotNull(dr.getSaveRuleId());
        assertNotNull(dr.getDiceFormulaId());

        // резолюция — спасбросок по DEX
        ArgumentCaptor<FeatureResolutionRule> resCaptor = ArgumentCaptor.forClass(FeatureResolutionRule.class);
        verify(resolutionRuleRepository).save(resCaptor.capture());
        assertEquals("saving_throw", resCaptor.getValue().getResolutionType());
        assertEquals(dex.getId(), resCaptor.getValue().getAbilityId());

        verify(healingRuleRepository).save(any(FeatureHealingRule.class));
    }

    @Test
    @DisplayName("sync: без полей механики — правила не создаются")
    void sync_noMechanics_createsNothing() {
        UUID spellId = UUID.randomUUID();
        Spell spell = new Spell();
        spell.setId(spellId);
        HomebrewPackage pkg = HomebrewPackage.builder().title("Pkg").build();
        pkg.setId(UUID.randomUUID());
        spell.setHomebrew(pkg);

        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("SPELL"), eq(spellId)))
                .thenReturn(List.of());

        service.sync(spell, pkg, new HomebrewSpellRequest(), "gm");

        verify(ruleRepository, never()).save(any());
        verify(revisionService, never()).approveCurrent(any(), any(), any());
    }
}
