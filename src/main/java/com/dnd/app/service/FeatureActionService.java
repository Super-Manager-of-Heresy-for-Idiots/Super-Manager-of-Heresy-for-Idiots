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
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс FeatureActionService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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
    private final CombatActionEconomyService economyService;

    /**
     * Возвращает список для операции "list available actions" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<AvailableFeatureAction> listAvailableActions(PlayerCharacter character) {
        return listAvailableActions(character, null);
    }

    /**
     * Возвращает доступные боевые действия от классовых умений с учетом ресурсов и слота действия.
     *
     * @param character персонаж, для которого собираются известные классовые умения
     * @param combatId идентификатор боя; если задан, доступность учитывает action economy комбатанта
     * @return список автоматизируемых и manual-only умений для боевой панели
     */
    @Transactional(readOnly = true)
    public List<AvailableFeatureAction> listAvailableActions(PlayerCharacter character, UUID combatId) {
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
        Map<UUID, List<FeatureRule>> rulesByFeature = rules.stream()
                .collect(Collectors.groupingBy(FeatureRule::getOwnerId));
        Map<UUID, FeatureRule> ruleById = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, Function.identity(), (a, b) -> a));
        List<UUID> ruleIds = List.copyOf(ruleById.keySet());

        List<FeatureActionCost> costs = ruleIds.isEmpty()
                ? List.of()
                : actionCostRepository.findByFeatureRuleIdIn(ruleIds);

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
        List<AvailableFeatureAction> result = new ArrayList<>();

        for (FeatureActionCost cost : costs) {
            FeatureRule rule = ruleById.get(cost.getFeatureRuleId());
            if (rule == null) {
                continue;
            }
            ClassFeature feature = featureById.get(rule.getOwnerId());
            if (feature == null) {
                continue;
            }
            ActionType at = actionTypes.get(cost.getActionTypeId());
            List<FeatureRule> featureRules = rulesByFeature.getOrDefault(feature.getId(), List.of());
            boolean executable = hasExecutableRules(featureRules);
            boolean manualOnly = isManualOnly(featureRules, executable);

            AvailableFeatureAction.AvailableFeatureActionBuilder b = AvailableFeatureAction.builder()
                    .featureId(feature.getId())
                    .featureName(feature.getTitle())
                    .featureRuleId(rule.getId())
                    .actionType(at != null ? at.getCode() : null)
                    .actionTypeLabel(at != null ? at.getDisplayName() : null)
                    .available(!manualOnly)
                    .unavailableReason(manualOnly ? "Правила ещё не оцифрованы" : null)
                    .hasExecutableRules(executable)
                    .manualOnly(manualOnly)
                    .requiresTarget(false)
                    .requiresConfirmation(false);

            if (!manualOnly && combatId != null && at != null
                    && !economyService.canSpend(combatId, character.getId(), at.getCode())) {
                b.available(false).unavailableReason(unavailableSlotReason(at.getCode()));
            }

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
            result.add(b.build());
        }

        Set<UUID> featuresWithRules = rulesByFeature.keySet();
        featureById.values().stream()
                .filter(feature -> !featuresWithRules.contains(feature.getId()))
                .forEach(feature -> result.add(AvailableFeatureAction.builder()
                        .featureId(feature.getId())
                        .featureName(feature.getTitle())
                        .available(false)
                        .unavailableReason("Правила ещё не оцифрованы")
                        .hasExecutableRules(false)
                        .manualOnly(true)
                        .requiresTarget(false)
                        .requiresConfirmation(false)
                        .build()));

        return result;
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

    private boolean hasExecutableRules(List<FeatureRule> rules) {
        return rules.stream().anyMatch(rule -> {
            String type = rule.getRuleType();
            return FeatureRuleProfile.DAMAGE.getCode().equals(type)
                    || FeatureRuleProfile.HEALING.getCode().equals(type)
                    || FeatureRuleProfile.SAVE_CHECK_ATTACK.getCode().equals(type)
                    || FeatureRuleProfile.ACTIVE_EFFECT.getCode().equals(type);
        });
    }

    private boolean isManualOnly(List<FeatureRule> rules, boolean executable) {
        return rules.isEmpty()
                || !executable
                || rules.stream().anyMatch(rule ->
                FeatureRuleProfile.MANUAL_ADJUDICATION.getCode().equals(rule.getRuleType()));
    }

    private String unavailableSlotReason(String actionTypeCode) {
        return switch (actionTypeCode) {
            case "action" -> "Действие уже потрачено";
            case "bonus_action" -> "Бонусное действие уже потрачено";
            case "reaction" -> "Реакция уже использована";
            default -> "Недоступно в текущем ходу";
        };
    }
}
