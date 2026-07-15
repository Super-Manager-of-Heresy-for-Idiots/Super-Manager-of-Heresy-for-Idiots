package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.ItemTemplateRepository;
import com.dnd.app.repository.MagicItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeatureRuleValidator {

    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureItemBindingRepository itemBindingRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final MagicItemRepository magicItemRepository;
    private final ItemTemplateRepository itemTemplateRepository;

    /**
     * Проверяет правило перед ручной валидацией или approve.
     * Блокирующие проблемы (error) складываются в {@code problems} и делают правило невалидным;
     * незаблокирующие замечания (warning) — в {@code warnings} и на валидность не влияют.
     * @param rule правило, которое проверяется
     * @return результат проверки с блокирующими проблемами и предупреждениями
     */
    public FeatureRuleValidationResponse validate(FeatureRule rule) {
        List<String> problems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (FeatureRuleProfile.fromCode(rule.getRuleType()).isEmpty()) {
            problems.add("Неизвестный тип правила: " + rule.getRuleType());
        }
        if (issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(rule.getId(), ERROR)) {
            problems.add("Есть неразрешённая ошибка (error), связанная с правилом");
        }
        validateItemRule(rule, problems, warnings);
        return FeatureRuleValidationResponse.builder()
                .valid(problems.isEmpty())
                .problems(problems)
                .warnings(warnings)
                .build();
    }

    /**
     * Проверяет структурные инварианты item-правил (ITEM_ABIL Фаза 1, §2.3).
     * @param rule правило feature-rules, которое проходит validate/approve
     * @param problems список блокирующих ошибок валидации (severity error)
     * @param warnings список незаблокирующих предупреждений (severity warning)
     */
    private void validateItemRule(FeatureRule rule, List<String> problems, List<String> warnings) {
        Optional<FeatureRuleOwnerType> ownerType = FeatureRuleOwnerType.fromCode(rule.getOwnerType());
        boolean itemOwner = ownerType.map(type -> type.isItemOwner()).orElse(false);
        List<FeatureResourceDefinition> resources = resourceDefinitionRepository.findByFeatureRuleId(rule.getId());
        if (!itemOwner) {
            boolean hasItemScopedResource = resources.stream()
                    .anyMatch(def -> def.getScope() == FeatureResourceScope.ITEM_INSTANCE);
            if (hasItemScopedResource) {
                problems.add("ITEM_INSTANCE ресурс допустим только у item-owner правила");
            }
            return;
        }

        Optional<FeatureItemBinding> binding = itemBindingRepository.findByFeatureRuleId(rule.getId());
        if (binding.isEmpty()) {
            problems.add("Item-правило должно иметь feature_item_binding");
        } else {
            FeatureItemBinding b = binding.get();
            if (b.isConsumeOnUse() && b.isRequiresEquipped()) {
                problems.add("consume_on_use несовместим с requires_equipped");
            }
            // (b) requires_attunement допустим только если определение предмета поддерживает аттюнмент.
            if (b.isRequiresAttunement() && !definitionSupportsAttunement(ownerType.orElse(null), rule.getOwnerId())) {
                warnings.add("requires_attunement указан, но определение предмета не поддерживает аттюнмент");
            }
        }

        // Реверс (d): у item-правила ресурс либо ITEM_INSTANCE-скоупа, либо отсутствует;
        // character-скоуп у item-правила теоретически возможен, но подозрителен — warning.
        boolean hasCharacterScopedResource = resources.stream()
                .anyMatch(def -> def.getScope() != FeatureResourceScope.ITEM_INSTANCE);
        if (hasCharacterScopedResource) {
            warnings.add("У item-правила ресурс с character-скоупом; обычно ожидается scope=ITEM_INSTANCE");
        }
    }

    /**
     * Определяет, поддерживает ли ОПРЕДЕЛЕНИЕ предмета аттюнмент.
     * Для {@code ITEM_MAGIC} смотрит {@code magic_item.attunement_required},
     * для {@code ITEM_TEMPLATE} — {@code item_templates.attunement_required};
     * {@code ITEM_EQUIPMENT} аттюнмент не поддерживает.
     * @param ownerType тип владельца правила (item-семейство)
     * @param ownerId идентификатор определения предмета ({@code owner_id})
     * @return true, если предмет может быть настроен (аттюнмент допустим)
     */
    private boolean definitionSupportsAttunement(FeatureRuleOwnerType ownerType, java.util.UUID ownerId) {
        if (ownerType == null || ownerId == null) {
            return false;
        }
        return switch (ownerType) {
            case ITEM_MAGIC -> magicItemRepository.findById(ownerId)
                    .map(mi -> Boolean.TRUE.equals(mi.getAttunementRequired()))
                    .orElse(false);
            case ITEM_TEMPLATE -> itemTemplateRepository.findById(ownerId)
                    .map(it -> Boolean.TRUE.equals(it.getAttunementRequired()))
                    .orElse(false);
            default -> false;
        };
    }
}
