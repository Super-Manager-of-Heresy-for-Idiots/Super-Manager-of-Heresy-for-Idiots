package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A single {@code feature_rule} row for the admin Rule Workbench. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleResponse {
    private UUID id;
    private String ownerType;
    private UUID ownerId;
    private String ruleType;
    private String ruleTypeLabel;
    private boolean enabled;
    private String reviewStatus;
    private Double confidence;
    private String source;
    private Integer sortOrder;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    // ── Versioning + scope (Stage 2) ──
    private UUID currentRevisionId;
    private UUID approvedRevisionId;
    private Integer currentRevisionNumber;
    private Integer approvedRevisionNumber;
    private int revisionCount;
    private UUID rulesetId;
    private UUID ruleSourceId;
    private Integer priority;

    /** Count of unresolved issues linked to this specific rule. */
    private long openIssueCount;
    /** True if this rule has an unresolved error issue (blocks approval). */
    private boolean hasUnresolvedError;
}
