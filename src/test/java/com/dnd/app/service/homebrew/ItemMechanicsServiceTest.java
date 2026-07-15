package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureItemBindingRepository;
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
 * Тест ItemMechanicsServiceTest (IT-4): GM-ввод механики предмета → approved ITEM-owned feature_rules с обязательным
 * feature_item_binding на каждом правиле; damage связан с save-правилом.
 */
@ExtendWith(MockitoExtension.class)
class ItemMechanicsServiceTest {

    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureDamageRuleRepository damageRuleRepository;
    @Mock private FeatureHealingRuleRepository healingRuleRepository;
    @Mock private FeatureResolutionRuleRepository resolutionRuleRepository;
    @Mock private FeatureItemBindingRepository itemBindingRepository;
    @Mock private FeatureRuleRevisionRepository revisionRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private FeatureRuleRevisionService revisionService;
    @Mock private DamageTypeRepository damageTypeRepository;
    @Mock private StatTypeRepository statTypeRepository;

    @InjectMocks private ItemMechanicsService service;

    @Test
    @DisplayName("sync(ITEM_MAGIC): урон+save+лечение → 3 approved правила, каждое с binding; damage→save связан")
    void sync_buildsRulesWithBindings() {
        UUID itemId = UUID.randomUUID();
        HomebrewPackage pkg = HomebrewPackage.builder().title("Pkg").build();
        pkg.setId(UUID.randomUUID());

        StatType dex = new StatType();
        dex.setId(UUID.randomUUID());
        dex.setSlug("dex");
        DamageType fire = new DamageType();
        fire.setId(UUID.randomUUID());
        fire.setSlug("fire");

        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("ITEM_MAGIC"), eq(itemId)))
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

        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setAbilityDamageDice("8d6");
        req.setAbilityDamageType("fire");
        req.setAbilitySaveAbility("dex");
        req.setAbilityHalfOnSave(true);
        req.setAbilityHealingFormula("2d8");
        req.setAbilityRequiresAttunement(true);

        service.sync(FeatureRuleOwnerType.ITEM_MAGIC, itemId, pkg, req, "gm");

        ArgumentCaptor<FeatureRule> ruleCaptor = ArgumentCaptor.forClass(FeatureRule.class);
        verify(ruleRepository, times(3)).save(ruleCaptor.capture());
        assertTrue(ruleCaptor.getAllValues().stream().allMatch(r -> "ITEM_MAGIC".equals(r.getOwnerType())));
        assertTrue(ruleCaptor.getAllValues().stream().allMatch(r -> itemId.equals(r.getOwnerId())));

        // Каждое item-правило обязано иметь binding (инвариант валидатора).
        ArgumentCaptor<FeatureItemBinding> bindCaptor = ArgumentCaptor.forClass(FeatureItemBinding.class);
        verify(itemBindingRepository, times(3)).save(bindCaptor.capture());
        assertTrue(bindCaptor.getAllValues().stream().allMatch(FeatureItemBinding::isRequiresAttunement));

        // 3 approve
        verify(revisionService, times(3)).approveCurrent(any(), any(), eq("gm"));

        // damage: тип урона + связь с save + half
        ArgumentCaptor<FeatureDamageRule> dmgCaptor = ArgumentCaptor.forClass(FeatureDamageRule.class);
        verify(damageRuleRepository).save(dmgCaptor.capture());
        FeatureDamageRule dr = dmgCaptor.getValue();
        assertEquals(fire.getId(), dr.getDamageTypeId());
        assertTrue(dr.isHalfOnSave());
        assertNotNull(dr.getSaveRuleId());

        verify(healingRuleRepository).save(any(FeatureHealingRule.class));
    }

    @Test
    @DisplayName("sync: без полей умения — правила не создаются")
    void sync_noAbility_createsNothing() {
        UUID itemId = UUID.randomUUID();
        HomebrewPackage pkg = HomebrewPackage.builder().title("Pkg").build();
        pkg.setId(UUID.randomUUID());
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("ITEM_MAGIC"), eq(itemId)))
                .thenReturn(List.of());

        service.sync(FeatureRuleOwnerType.ITEM_MAGIC, itemId, pkg, new HomebrewItemRequest(), "gm");

        verify(ruleRepository, never()).save(any());
        verify(itemBindingRepository, never()).save(any());
    }
}
