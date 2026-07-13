package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.dto.response.ItemAbilitySummary;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Резолвит approved item-правила в пары "правило × конкретный экземпляр предмета".
 */
@Service
@RequiredArgsConstructor
public class ItemAbilityResolver {

    private final FeatureRulesProperties flags;
    private final ItemInstanceRepository itemInstanceRepository;
    private final CharacterFeatureResolver characterFeatureResolver;
    private final FeatureItemBindingRepository bindingRepository;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final ItemInstanceFeatureResourceRepository itemResourceRepository;

    /**
     * Активное item-правило с binding и экземпляром-источником.
     * @param rule approved+enabled правило
     * @param binding условия активности правила
     * @param instance экземпляр предмета, который даёт правило
     */
    public record ActiveItemRule(FeatureRule rule, FeatureItemBinding binding, ItemInstance instance) {
    }

    /**
     * Возвращает активные item-правила персонажа с учётом состояния конкретного экземпляра.
     * @param character персонаж-владелец инвентаря
     * @return список активных пар правило × экземпляр
     */
    @Transactional(readOnly = true)
    public List<ActiveItemRule> resolveActiveRules(PlayerCharacter character) {
        if (!flags.itemsActive()) {
            return List.of();
        }
        return resolveActiveRules(itemInstanceRepository.findByOwnerCharacterId(character.getId()));
    }

    /**
     * Возвращает активные item-правила одного экземпляра предмета.
     * @param instance экземпляр предмета
     * @return список активных item-правил
     */
    @Transactional(readOnly = true)
    public List<ActiveItemRule> resolveActiveRules(ItemInstance instance) {
        if (!flags.itemsActive()) {
            return List.of();
        }
        return resolveActiveRules(List.of(instance));
    }

    /**
     * Строит DTO умений предметов, сгруппированные по id экземпляра.
     * @param character персонаж-владелец инвентаря
     * @return карта instanceId -> список кратких умений
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<ItemAbilitySummary>> summariesByInstance(PlayerCharacter character) {
        List<ActiveItemRule> activeRules = resolveActiveRules(character);
        if (activeRules.isEmpty()) {
            return Map.of();
        }
        List<UUID> ruleIds = activeRules.stream().map(r -> r.rule().getId()).distinct().toList();
        Map<UUID, FeatureActionCost> costByRule = actionCostRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .collect(Collectors.toMap(FeatureActionCost::getFeatureRuleId, Function.identity(), (a, b) -> a));
        Map<UUID, ActionType> actionTypeById = actionTypeRepository.findByIdIn(
                        costByRule.values().stream().map(FeatureActionCost::getActionTypeId).distinct().toList())
                .stream().collect(Collectors.toMap(ActionType::getId, Function.identity(), (a, b) -> a));
        Map<UUID, FeatureResourceDefinition> itemDefByRule =
                resourceDefinitionRepository.findByFeatureRuleIdIn(ruleIds).stream()
                        .filter(def -> def.getScope() == FeatureResourceScope.ITEM_INSTANCE)
                        .collect(Collectors.toMap(FeatureResourceDefinition::getFeatureRuleId,
                                Function.identity(), (a, b) -> a));
        Map<String, ItemInstanceFeatureResource> resourceByInstanceAndDef =
                itemResourceRepository.findByItemInstanceIdIn(activeRules.stream()
                                .map(r -> r.instance().getId()).distinct().toList()).stream()
                        .collect(Collectors.toMap(
                                r -> key(r.getItemInstanceId(), r.getResourceDefinitionId()),
                                Function.identity(),
                                (a, b) -> a));

        Map<UUID, List<ItemAbilitySummary>> out = new HashMap<>();
        for (ActiveItemRule active : activeRules) {
            FeatureActionCost cost = costByRule.get(active.rule().getId());
            ActionType actionType = cost != null ? actionTypeById.get(cost.getActionTypeId()) : null;
            FeatureResourceDefinition resourceDef = itemDefByRule.get(active.rule().getId());
            ItemAbilitySummary.Charges charges = null;
            if (resourceDef != null) {
                ItemInstanceFeatureResource resource = resourceByInstanceAndDef.get(
                        key(active.instance().getId(), resourceDef.getId()));
                charges = ItemAbilitySummary.Charges.builder()
                        .current(resource != null ? resource.getCurrentValue() : 0)
                        .max(resource != null ? resource.getMaxValueSnapshot() : null)
                        .build();
            }
            out.computeIfAbsent(active.instance().getId(), ignored -> new ArrayList<>()).add(
                    ItemAbilitySummary.builder()
                            .ruleId(active.rule().getId())
                            .name(active.rule().getRuleType())
                            .actionType(actionType != null ? actionType.getCode() : null)
                            .charges(charges)
                            .consumesItem(active.binding().isConsumeOnUse())
                            .requiresAttunement(active.binding().isRequiresAttunement())
                            .requiresEquipped(active.binding().isRequiresEquipped())
                            .available(true)
                            .build());
        }
        return out;
    }

    private List<ActiveItemRule> resolveActiveRules(Collection<ItemInstance> instances) {
        if (instances.isEmpty()) {
            return List.of();
        }
        Map<FeatureRuleOwnerType, List<UUID>> ownerIds = new HashMap<>();
        for (ItemInstance instance : instances) {
            FeatureRuleOwnerType ownerType = ownerType(instance);
            UUID ownerId = instance.getReferenceId();
            if (ownerType != null && ownerId != null) {
                ownerIds.computeIfAbsent(ownerType, ignored -> new ArrayList<>()).add(ownerId);
            }
        }
        if (ownerIds.isEmpty()) {
            return List.of();
        }

        List<FeatureRule> rules = new ArrayList<>();
        ownerIds.forEach((ownerType, ids) -> rules.addAll(characterFeatureResolver.approvedEnabledRules(ownerType, ids)));
        if (rules.isEmpty()) {
            return List.of();
        }
        Map<UUID, FeatureItemBinding> bindingByRule = bindingRepository.findByFeatureRuleIdIn(
                        rules.stream().map(FeatureRule::getId).toList()).stream()
                .collect(Collectors.toMap(FeatureItemBinding::getFeatureRuleId, Function.identity(), (a, b) -> a));
        Map<String, List<FeatureRule>> rulesByOwner = rules.stream()
                .collect(Collectors.groupingBy(rule -> rule.getOwnerType() + ":" + rule.getOwnerId()));

        List<ActiveItemRule> out = new ArrayList<>();
        for (ItemInstance instance : instances) {
            FeatureRuleOwnerType ownerType = ownerType(instance);
            UUID ownerId = instance.getReferenceId();
            if (ownerType == null || ownerId == null) {
                continue;
            }
            for (FeatureRule rule : rulesByOwner.getOrDefault(ownerType.getCode() + ":" + ownerId, List.of())) {
                FeatureItemBinding binding = bindingByRule.get(rule.getId());
                if (binding != null && bindingSatisfied(instance, binding)) {
                    out.add(new ActiveItemRule(rule, binding, instance));
                }
            }
        }
        return out;
    }

    private boolean bindingSatisfied(ItemInstance instance, FeatureItemBinding binding) {
        if (binding.isRequiresEquipped()) {
            if (instance.getSlot() == null) {
                return false;
            }
            if (binding.getAllowedSlotCode() != null
                    && !binding.getAllowedSlotCode().isBlank()
                    && !binding.getAllowedSlotCode().equalsIgnoreCase(instance.getSlot().getCode())) {
                return false;
            }
        }
        if (binding.isRequiresAttunement() && !Boolean.TRUE.equals(instance.getAttuned())) {
            return false;
        }
        return !binding.isConsumeOnUse() || (instance.getQuantity() != null && instance.getQuantity() > 0);
    }

    private FeatureRuleOwnerType ownerType(ItemInstance instance) {
        if (instance.getTemplate() != null) {
            return FeatureRuleOwnerType.ITEM_TEMPLATE;
        }
        if (instance.getEquipmentItem() != null) {
            return FeatureRuleOwnerType.ITEM_EQUIPMENT;
        }
        if (instance.getMagicItem() != null) {
            return FeatureRuleOwnerType.ITEM_MAGIC;
        }
        return null;
    }

    private String key(UUID itemInstanceId, UUID resourceDefinitionId) {
        return itemInstanceId + ":" + resourceDefinitionId;
    }
}
