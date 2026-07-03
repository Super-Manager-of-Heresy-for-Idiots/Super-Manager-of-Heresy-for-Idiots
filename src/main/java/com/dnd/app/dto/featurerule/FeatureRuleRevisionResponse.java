package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** One immutable revision of a rule, for the history / compare / rollback UI. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleRevisionResponse {
    private UUID id;
    private UUID featureRuleId;
    private Integer revisionNumber;
    private String status;
    private String rulePayloadSnapshot;
    private String changeReason;
    private UUID createdBy;
    private Instant createdAt;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID disabledBy;
    private Instant disabledAt;

    /** True if this is the rule's current (editable) revision. */
    private boolean current;
    /** True if this is the rule's active approved revision (the one the runtime executes). */
    private boolean approvedActive;
}
