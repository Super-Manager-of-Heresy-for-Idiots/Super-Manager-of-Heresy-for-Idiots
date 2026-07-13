package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Создаёт и чистит runtime-состояние умений, которые предметы дают через feature-rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAbilityProvisioningService {

    private final FeatureRulesProperties flags;
    private final ItemAbilityResolver itemAbilityResolver;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final ItemInstanceFeatureResourceRepository itemResourceRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final FeatureActiveEffectRepository activeEffectRepository;

    /**
     * Идемпотентно создаёт недостающие item-scoped ресурсы для активных правил экземпляра.
     * @param instance экземпляр предмета, для которого создаются ресурсы
     */
    @Transactional
    public void ensureInstanceResources(ItemInstance instance) {
        if (!flags.itemsActive() || instance.getOwnerCharacter() == null) {
            return;
        }
        List<UUID> ruleIds = itemAbilityResolver.resolveActiveRules(instance).stream()
                .map(active -> active.rule().getId())
                .toList();
        if (ruleIds.isEmpty()) {
            return;
        }
        FormulaContext ctx = contextFactory.build(instance.getOwnerCharacter());
        for (FeatureResourceDefinition def : resourceDefinitionRepository.findByFeatureRuleIdIn(ruleIds)) {
            if (def.getScope() != FeatureResourceScope.ITEM_INSTANCE) {
                continue;
            }
            if (itemResourceRepository.findByItemInstanceIdAndResourceDefinitionId(instance.getId(), def.getId()).isPresent()) {
                continue;
            }
            Integer max = computeMax(def, ctx);
            int initial = max != null ? max : 0;
            itemResourceRepository.save(ItemInstanceFeatureResource.builder()
                    .itemInstanceId(instance.getId())
                    .resourceDefinitionId(def.getId())
                    .currentValue(initial)
                    .maxValueSnapshot(initial)
                    .build());
        }
    }

    /**
     * Помечает активные feature-rules эффекты экземпляра предмета истёкшими.
     * @param itemInstanceId id экземпляра предмета-источника
     */
    @Transactional
    public void expireInstanceEffects(UUID itemInstanceId) {
        if (!flags.itemsActive()) {
            return;
        }
        activeEffectRepository.expireActiveBySourceItemInstanceId(
                itemInstanceId,
                ActiveEffectStatus.ACTIVE.getCode(),
                ActiveEffectStatus.EXPIRED.getCode());
    }

    private Integer computeMax(FeatureResourceDefinition def, FormulaContext ctx) {
        if (def.getMaxFormulaId() == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(def.getMaxFormulaId()).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return Math.max(0, formulaService.evaluateInteger(formula, ctx));
        } catch (FormulaException e) {
            log.warn("Item resource max formula failed for definition {}: {}", def.getId(), e.getMessage());
            return null;
        }
    }
}
