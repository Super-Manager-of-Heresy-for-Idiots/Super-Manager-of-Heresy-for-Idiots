package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Admin edit of a feature ACTION_COST rule (action-economy cost to use the feature). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCostEditRequest {
    private UUID actionTypeId;
    private Integer amount;
    /** Optional predicate (boolean DSL) gating whether the cost applies; blank = always. */
    @Size(max = 2000)
    private String conditionFormula;
}
