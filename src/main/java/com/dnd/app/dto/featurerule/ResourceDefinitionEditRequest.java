package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin edit of a feature RESOURCE rule's definition: what the resource is called, how its maximum is
 * computed (a DSL formula, e.g. {@code ability_mod("INT")}), and when it resets. Used by the Rule Workbench
 * resource editor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDefinitionEditRequest {

    @Size(max = 64)
    private String resourceKey;

    @Size(max = 120)
    private String displayName;

    /** DSL expression for the max value (blank = no formula / manual max). */
    @Size(max = 2000)
    private String maxFormula;

    /** rest_type code the resource resets on (e.g. short_rest, long_rest); blank = no reset. */
    @Size(max = 32)
    private String resetRestType;

    /** Optional partial-rest recovery formula. Blank means reset fully restores to max. */
    @Size(max = 2000)
    private String resetAmountFormula;

    /** Optional spend-per-use formula for action integrations. */
    @Size(max = 2000)
    private String spendPerUseFormula;

    private boolean allowNegative;

    @Size(max = 64)
    private String sharedPoolKey;
}
