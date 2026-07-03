package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Where a {@code feature_rule} came from (plan §4.1). This is a technical provenance marker and is
 * intentionally separate from the game-facing source/ruleset scope introduced in Stage 2.
 */
public enum FeatureRuleSource {

    /** Authored by an admin in the Rule Workbench. */
    MANUAL("manual"),
    /** Produced by the description parser/importer. */
    PARSER("parser"),
    /** Created by the content seed pipeline. */
    SEED("seed"),
    /** Created by a data migration/backfill. */
    MIGRATION("migration");

    private final String code;

    FeatureRuleSource(String code) {
        this.code = code;
    }

    /** Stable identifier stored in the {@code source} column. */
    public String getCode() {
        return code;
    }

    /** Resolve a source from its persisted {@link #getCode() code}. */
    public static Optional<FeatureRuleSource> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
