package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Partial update of a rule's editable fields. Null fields are left unchanged. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureRuleRequest {

    @Size(max = 48)
    private String ruleType;

    private Boolean enabled;

    private Integer sortOrder;

    @Size(max = 4000)
    private String notes;

    private Double confidence;

    // ── Game source / ruleset scope (Stage 2) ──
    private UUID rulesetId;
    private UUID ruleSourceId;
    private Integer priority;

    @Size(max = 2000)
    private String changeReason;
}
