package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * What a {@code feature_rule} / {@code feature_rule_issue} is attached to.
 *
 * <p>Only {@link #CLASS_FEATURE} is allowed today (first wave). The {@code owner_type}/{@code owner_id}
 * shape exists so the same rules runtime can later be reused for race features, feats, items and
 * homebrew without reshaping the tables — see {@code anal-integration/01_STAGE_RULE_SCHEMA_FOUNDATION.md}.</p>
 */
public enum FeatureRuleOwnerType {

    CLASS_FEATURE("CLASS_FEATURE");

    private final String code;

    FeatureRuleOwnerType(String code) {
        this.code = code;
    }

    /** Stable identifier stored in the {@code owner_type} column. */
    public String getCode() {
        return code;
    }

    /** Resolve an owner type from its persisted {@link #getCode() code}. */
    public static Optional<FeatureRuleOwnerType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
