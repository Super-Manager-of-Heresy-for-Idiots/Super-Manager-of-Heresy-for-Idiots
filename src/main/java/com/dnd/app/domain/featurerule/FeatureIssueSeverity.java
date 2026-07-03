package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Severity of a {@code feature_rule_issue} (see
 * {@code anal-integration/00_STAGE_SCOPE_AND_EXECUTION_CONTRACT.md}).
 *
 * <p>An {@link #ERROR} issue that is still unresolved blocks approval of the related rule: a rule
 * cannot be moved to {@link FeatureReviewStatus#APPROVED} while it has an unresolved error.</p>
 */
public enum FeatureIssueSeverity {

    /** Informational note; does not block anything. */
    INFO("info"),
    /** Warning; should be reviewed but does not block approval. */
    WARN("warn"),
    /** Blocking problem; an unresolved error prevents approval of the rule. */
    ERROR("error");

    private final String code;

    FeatureIssueSeverity(String code) {
        this.code = code;
    }

    /** Stable snake_case identifier used in persistence and API payloads. */
    public String getCode() {
        return code;
    }

    /** True for severities whose unresolved presence must block rule approval. */
    public boolean blocksApproval() {
        return this == ERROR;
    }

    /** Resolve a severity from its persisted {@link #getCode() code}. */
    public static Optional<FeatureIssueSeverity> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
