package com.dnd.app.domain.featurerule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureRuleOwnerTypeTest {

    @Test
    void itemFamilyContainsAllItemOwners() {
        assertThat(FeatureRuleOwnerType.ITEM_FAMILY)
                .containsExactlyInAnyOrder(
                        FeatureRuleOwnerType.ITEM_TEMPLATE,
                        FeatureRuleOwnerType.ITEM_EQUIPMENT,
                        FeatureRuleOwnerType.ITEM_MAGIC);
        assertThat(FeatureRuleOwnerType.ITEM_MAGIC.isItemOwner()).isTrue();
        assertThat(FeatureRuleOwnerType.CLASS_FEATURE.isItemOwner()).isFalse();
    }
}
