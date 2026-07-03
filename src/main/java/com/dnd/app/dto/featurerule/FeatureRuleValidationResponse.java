package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of validating a rule. Stage 1 validation is intentionally lightweight (known rule type,
 * no unresolved error issue, owner exists); later stages add formula/reference checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleValidationResponse {
    /** True if the rule currently passes validation and could be approved. */
    private boolean valid;
    /** Human-readable problems blocking validity (empty when valid). */
    private List<String> problems;
}
