package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты резолвера item-правил: акцент на легаси-гварде {@code hasApprovedItemRule},
 * который блокирует старый путь use-item при наличии approved-правила.
 */
@ExtendWith(MockitoExtension.class)
class ItemAbilityResolverTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private ItemInstanceRepository itemInstanceRepository;
    @Mock private CharacterFeatureResolver characterFeatureResolver;
    @Mock private FeatureItemBindingRepository bindingRepository;
    @Mock private FeatureActionCostRepository actionCostRepository;
    @Mock private ActionTypeRepository actionTypeRepository;
    @Mock private FeatureResourceDefinitionRepository resourceDefinitionRepository;
    @Mock private ItemInstanceFeatureResourceRepository itemResourceRepository;

    @InjectMocks private ItemAbilityResolver resolver;

    @Test
    void hasApprovedItemRuleFalseWhenItemsInactive() {
        when(flags.itemsActive()).thenReturn(false);

        assertThat(resolver.hasApprovedItemRule(mock(ItemInstance.class))).isFalse();
    }

    @Test
    void hasApprovedItemRuleTrueWhenApprovedRuleExists() {
        when(flags.itemsActive()).thenReturn(true);
        ItemInstance instance = mock(ItemInstance.class);
        UUID defId = UUID.randomUUID();
        when(instance.getMagicItem()).thenReturn(mock(MagicItem.class));
        when(instance.getReferenceId()).thenReturn(defId);
        when(characterFeatureResolver.approvedEnabledRules(FeatureRuleOwnerType.ITEM_MAGIC, List.of(defId)))
                .thenReturn(List.of(FeatureRule.builder().build()));

        assertThat(resolver.hasApprovedItemRule(instance)).isTrue();
    }

    @Test
    void hasApprovedItemRuleFalseWhenNoRules() {
        when(flags.itemsActive()).thenReturn(true);
        ItemInstance instance = mock(ItemInstance.class);
        UUID defId = UUID.randomUUID();
        when(instance.getMagicItem()).thenReturn(mock(MagicItem.class));
        when(instance.getReferenceId()).thenReturn(defId);
        when(characterFeatureResolver.approvedEnabledRules(FeatureRuleOwnerType.ITEM_MAGIC, List.of(defId)))
                .thenReturn(List.of());

        assertThat(resolver.hasApprovedItemRule(instance)).isFalse();
    }
}
