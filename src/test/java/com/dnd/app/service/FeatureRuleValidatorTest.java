package com.dnd.app.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureRuleValidatorTest {

    @Mock private FeatureRuleIssueRepository issueRepository;
    @Mock private FeatureItemBindingRepository itemBindingRepository;
    @Mock private FeatureResourceDefinitionRepository resourceDefinitionRepository;

    @InjectMocks private FeatureRuleValidator validator;

    private final UUID ruleId = UUID.randomUUID();

    @Test
    void validateRejectsItemRuleWithoutBinding() {
        FeatureRule rule = rule(FeatureRuleOwnerType.ITEM_MAGIC);
        when(issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(any(), anyString())).thenReturn(false);
        when(resourceDefinitionRepository.findByFeatureRuleId(ruleId)).thenReturn(List.of());
        when(itemBindingRepository.findByFeatureRuleId(ruleId)).thenReturn(Optional.empty());

        FeatureRuleValidationResponse response = validator.validate(rule);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getProblems()).contains("Item-правило должно иметь feature_item_binding");
    }

    @Test
    void validateRejectsConsumableEquippedCombo() {
        FeatureRule rule = rule(FeatureRuleOwnerType.ITEM_TEMPLATE);
        when(issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(any(), anyString())).thenReturn(false);
        when(resourceDefinitionRepository.findByFeatureRuleId(ruleId)).thenReturn(List.of());
        when(itemBindingRepository.findByFeatureRuleId(ruleId)).thenReturn(Optional.of(FeatureItemBinding.builder()
                .featureRuleId(ruleId)
                .consumeOnUse(true)
                .requiresEquipped(true)
                .build()));

        FeatureRuleValidationResponse response = validator.validate(rule);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getProblems()).contains("consume_on_use несовместим с requires_equipped");
    }

    @Test
    void validateRejectsItemScopedResourceOnNonItemRule() {
        FeatureRule rule = rule(FeatureRuleOwnerType.CLASS_FEATURE);
        when(issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(any(), anyString())).thenReturn(false);
        when(resourceDefinitionRepository.findByFeatureRuleId(ruleId)).thenReturn(List.of(
                FeatureResourceDefinition.builder()
                        .featureRuleId(ruleId)
                        .scope(FeatureResourceScope.ITEM_INSTANCE)
                        .build()));

        FeatureRuleValidationResponse response = validator.validate(rule);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getProblems()).contains("ITEM_INSTANCE ресурс допустим только у item-owner правила");
    }

    private FeatureRule rule(FeatureRuleOwnerType ownerType) {
        return FeatureRule.builder()
                .id(ruleId)
                .ownerType(ownerType.getCode())
                .ownerId(UUID.randomUUID())
                .ruleType(FeatureRuleProfile.ACTION_COST.getCode())
                .build();
    }
}
