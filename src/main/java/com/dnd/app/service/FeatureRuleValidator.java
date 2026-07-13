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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общая лёгкая валидация правила feature-rules перед validate/approve.
 */
@Component
@RequiredArgsConstructor
public class FeatureRuleValidator {

    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureItemBindingRepository itemBindingRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;

    /**
     * Проверяет правило перед ручной валидацией или approve.
     * @param rule правило, которое проверяется
     * @return результат проверки с блокирующими проблемами
     */
    public FeatureRuleValidationResponse validate(FeatureRule rule) {
        List<String> problems = new ArrayList<>();
        if (FeatureRuleProfile.fromCode(rule.getRuleType()).isEmpty()) {
            problems.add("Неизвестный тип правила: " + rule.getRuleType());
        }
        if (issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(rule.getId(), ERROR)) {
            problems.add("Есть неразрешённая ошибка (error), связанная с правилом");
        }
        validateItemRule(rule, problems);
        return FeatureRuleValidationResponse.builder()
                .valid(problems.isEmpty())
                .problems(problems)
                .build();
    }

    /**
     * Проверяет структурные инварианты item-правил.
     * @param rule правило feature-rules, которое проходит validate/approve
     * @param problems список блокирующих ошибок валидации
     */
    private void validateItemRule(FeatureRule rule, List<String> problems) {
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
        } else if (binding.get().isConsumeOnUse() && binding.get().isRequiresEquipped()) {
            problems.add("consume_on_use несовместим с requires_equipped");
        }
    }
}
