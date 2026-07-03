package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Lifecycle of a feature rule (see {@code anal-integration/00_STAGE_SCOPE_AND_EXECUTION_CONTRACT.md}).
 *
 * <p>Flow: {@code draft -> needs_review -> approved | disabled}. Only {@link #APPROVED} rules are
 * eligible to be executed by the runtime; everything else is authoring/back-office state and never
 * affects gameplay.</p>
 */
public enum FeatureReviewStatus {

    /** Work in progress; not yet submitted for review. */
    DRAFT("draft"),
    /** Submitted / parser-generated; waiting for admin review. */
    NEEDS_REVIEW("needs_review"),
    /** Reviewed and approved; the only status eligible for runtime execution. */
    APPROVED("approved"),
    /** Explicitly turned off; kept for history but never executed. */
    DISABLED("disabled");

    private final String code;

    FeatureReviewStatus(String code) {
        this.code = code;
    }

    /** Stable snake_case identifier used in persistence and API payloads. */
    public String getCode() {
        return code;
    }

    /** True only for {@link #APPROVED}: the single status the runtime is allowed to execute. */
    public boolean isRuntimeEligible() {
        return this == APPROVED;
    }

    /** Resolve a status from its persisted {@link #getCode() code}. */
    public static Optional<FeatureReviewStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
