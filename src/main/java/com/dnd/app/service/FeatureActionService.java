package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.featurerule.AvailableFeatureAction;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Collects the feature actions a character can currently use, with action + resource cost/availability. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureActionService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;

    @Transactional(readOnly = true)
    public List<AvailableFeatureAction> listAvailableActions(PlayerCharacter character) {
        if (!flags.actionsActive()) {
            return List.of();
        }
        List<ClassFeature> features = resolver.knownBaseClassFeatures(character.getId());
        if (features.isEmpty()) {
            return List.of();
        }
        Map<UUID, ClassFeature> featureById = features.stream()
                .collect(Collectors.toMap(ClassFeature::getId, Function.identity(), (a, b) -> a));

        List<FeatureRule> rules = resolver.approvedEnabledRules(featureById.keySet());
        if (rules.isEmpty()) {
            return List.of();
        }
        Map<UUID, FeatureRule> ruleById = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, Function.identity(), (a, b) -> a));
        List<UUID> ruleIds = List.copyOf(ruleById.keySet());

        List<FeatureActionCost> costs = actionCostRepository.findByFeatureRuleIdIn(ruleIds);
        if (costs.isEmpty()) {
            return List.of();
        }

        Map<UUID, ActionType> actionTypes = actionTypeRepository.findByIdIn(
                costs.stream().map(FeatureActionCost::getActionTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(ActionType::getId, Function.identity(), (a, b) -> a));

        // first resource definition per rule (spend cost source)
        Map<UUID, FeatureResourceDefinition> defByRule = resourceDefinitionRepository.findByFeatureRuleIdIn(ruleIds)
                .stream().collect(Collectors.toMap(FeatureResourceDefinition::getFeatureRuleId,
                        Function.identity(), (a, b) -> a));

        Map<UUID, CharacterFeatureResource> resByDef = resourceRepository.findByCharacterId(character.getId())
                .stream().collect(Collectors.toMap(CharacterFeatureResource::getResourceDefinitionId,
                        Function.identity(), (a, b) -> a));

        FormulaContext ctx = contextFactory.build(character);

        return costs.stream().map(cost -> {
            FeatureRule rule = ruleById.get(cost.getFeatureRuleId());
            if (rule == null) {
                return null;
            }
            ClassFeature feature = featureById.get(rule.getOwnerId());
            if (feature == null) {
                return null;
            }
            ActionType at = actionTypes.get(cost.getActionTypeId());

            AvailableFeatureAction.AvailableFeatureActionBuilder b = AvailableFeatureAction.builder()
                    .featureId(feature.getId())
                    .featureName(feature.getTitle())
                    .featureRuleId(rule.getId())
                    .actionType(at != null ? at.getCode() : null)
                    .actionTypeLabel(at != null ? at.getDisplayName() : null)
                    .available(true)
                    .requiresTarget(false)
                    .requiresConfirmation(false);

            FeatureResourceDefinition def = defByRule.get(rule.getId());
            if (def != null) {
                int cst = spendPerUse(def, ctx);
                CharacterFeatureResource res = resByDef.get(def.getId());
                int remaining = res != null && res.getCurrentValue() != null ? res.getCurrentValue() : 0;
                b.resourceDefinitionId(def.getId()).resourceKey(def.getResourceKey())
                        .resourceCost(cst).resourceRemaining(remaining);
                if (remaining < cst && !def.isAllowNegative()) {
                    b.available(false).unavailableReason("Недостаточно ресурса");
                }
            }
            return b.build();
        }).filter(a -> a != null).toList();
    }

    int spendPerUse(FeatureResourceDefinition def, FormulaContext ctx) {
        if (def.getSpendPerUseFormulaId() == null) {
            return 1;
        }
        FeatureFormula formula = formulaRepository.findById(def.getSpendPerUseFormulaId()).orElse(null);
        if (formula == null) {
            return 1;
        }
        try {
            return Math.max(0, formulaService.evaluateInteger(formula, ctx));
        } catch (FormulaException e) {
            log.warn("Spend-per-use formula failed for definition {}: {}", def.getId(), e.getMessage());
            return 1;
        }
    }
}
