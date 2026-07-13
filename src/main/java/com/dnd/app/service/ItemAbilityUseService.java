package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.FeatureUseRequest;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAbilityUseService {

    private final FeatureRulesProperties flags;
    private final ItemInstanceRepository itemInstanceRepository;
    private final ItemAbilityResolver itemAbilityResolver;
    private final ItemAbilityProvisioningService itemAbilityProvisioningService;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final ItemInstanceFeatureResourceRepository itemResourceRepository;
    private final FeatureActionService featureActionService;
    private final FeatureEffectService featureEffectService;
    private final CombatFeatureExecutionService combatFeatureExecutionService;
    private final GameplayEventService gameplayEventService;
    private final CharacterFormulaContextFactory contextFactory;
    private final CombatActionEconomyService economyService;
    private final FeatureFormulaService formulaService;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureUseLogRepository useLogRepository;

    @Transactional(readOnly = true)
    public FeatureExecutionPlan plan(PlayerCharacter character, UUID itemInstanceId, UUID ruleId) {
        ItemInstance item = itemInstanceRepository.findById(itemInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(character.getId())) {
            throw new BadRequestException("This item is not carried by the active character");
        }
        ItemAbilityResolver.ActiveItemRule active = activeRule(item, ruleId);
        return combatFeatureExecutionService.planForItemRule(character, active.rule(), item.getDisplayName());
    }

    @Transactional
    public FeatureUseResult use(PlayerCharacter character, UUID itemInstanceId, UUID ruleId, FeatureUseRequest request) {
        if (!flags.actionsActive()) {
            throw new BadRequestException("Runtime использования умений отключён");
        }
        ItemInstance item = itemInstanceRepository.findByIdForUpdate(itemInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(character.getId())) {
            throw new BadRequestException("This item is not carried by the active character");
        }

        ItemAbilityResolver.ActiveItemRule active = activeRule(item, ruleId);
        itemAbilityProvisioningService.ensureInstanceResources(item);

        FeatureRule rule = active.rule();
        FeatureActionCost cost = actionCostRepository.findByFeatureRuleId(rule.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("У умения предмета нет стоимости действия (action cost)"));
        ActionType actionType = actionTypeRepository.findById(cost.getActionTypeId()).orElse(null);

        UUID combatId = request != null ? request.getCombatId() : null;
        if (combatId != null && actionType != null && costApplies(cost, character)) {
            economyService.spend(combatId, character.getId(), actionType.getCode());
        }

        Integer spent = null;
        Integer remaining = null;
        String resourceKey = null;
        FeatureResourceDefinition def = resourceDefinitionRepository.findByFeatureRuleId(rule.getId()).stream()
                .filter(d -> d.getScope() == FeatureResourceScope.ITEM_INSTANCE)
                .findFirst()
                .orElse(null);
        if (def != null) {
            FormulaContext ctx = contextFactory.build(character);
            int amount = featureActionService.spendPerUse(def, ctx);
            ItemInstanceFeatureResource resource = itemResourceRepository
                    .findByItemInstanceIdAndResourceDefinitionIdForUpdate(item.getId(), def.getId())
                    .orElseThrow(() -> new BadRequestException("Ресурс умения предмета не инициализирован"));
            int current = resource.getCurrentValue() != null ? resource.getCurrentValue() : 0;
            if (current < amount && !def.isAllowNegative()) {
                throw new BadRequestException("Недостаточно зарядов предмета");
            }
            resource.setCurrentValue(current - amount);
            itemResourceRepository.save(resource);
            spent = amount;
            remaining = resource.getCurrentValue();
            resourceKey = def.getResourceKey();
        }

        featureEffectService.applyForItemRuleUse(character, item.getId(), rule);
        if (active.binding().isConsumeOnUse()) {
            consumeUnits(item, active.binding().getConsumeQuantity());
        }

        gameplayEventService.publish(character, "feature_used", combatId,
                "{\"featureRuleId\":\"" + rule.getId() + "\",\"itemInstanceId\":\"" + itemInstanceId + "\"}");
        FeatureUseLog logEntry = useLogRepository.save(FeatureUseLog.builder()
                .characterId(character.getId())
                .featureRuleId(rule.getId())
                .combatId(combatId)
                .actionType(actionType != null ? actionType.getCode() : null)
                .resourceSpent(spent)
                .detail(item.getDisplayName() + ": " + rule.getRuleType())
                .build());

        return FeatureUseResult.builder()
                .featureId(rule.getId())
                .featureName(item.getDisplayName())
                .actionType(actionType != null ? actionType.getCode() : null)
                .resourceKey(resourceKey)
                .resourceSpent(spent)
                .resourceRemaining(remaining)
                .logId(logEntry.getId())
                .message("Умение предмета использовано")
                .build();
    }

    private ItemAbilityResolver.ActiveItemRule activeRule(ItemInstance item, UUID ruleId) {
        return itemAbilityResolver.resolveActiveRules(item).stream()
                .filter(r -> r.rule().getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("The item does not provide this active ability"));
    }

    private boolean costApplies(FeatureActionCost cost, PlayerCharacter character) {
        if (cost.getConditionFormulaId() == null) {
            return true;
        }
        FeatureFormula formula = formulaRepository.findById(cost.getConditionFormulaId()).orElse(null);
        if (formula == null) {
            return true;
        }
        try {
            return formulaService.evaluateBoolean(formula, contextFactory.build(character));
        } catch (FormulaException e) {
            log.warn("Item action-cost condition formula failed for {}: {}", cost.getId(), e.getMessage());
            return true;
        }
    }

    private void consumeUnits(ItemInstance item, Integer amount) {
        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        int consumed = amount != null && amount > 0 ? amount : 1;
        if (quantity <= consumed) {
            itemInstanceRepository.delete(item);
        } else {
            item.setQuantity(quantity - consumed);
            itemInstanceRepository.save(item);
        }
    }
}
