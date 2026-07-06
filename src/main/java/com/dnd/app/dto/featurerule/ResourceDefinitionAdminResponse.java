package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature RESOURCE rule's definition for the admin editor (with the max formula + its validation status). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDefinitionAdminResponse {
    private UUID id;
    private UUID featureRuleId;
    private String resourceKey;
    private String displayName;
    private String maxFormula;
    /** valid | invalid | unknown */
    private String maxFormulaStatus;
    private String maxFormulaMessage;
    private String resetRestType;
    private String resetAmountFormula;
    private String resetAmountFormulaStatus;
    private String resetAmountFormulaMessage;
    private String spendPerUseFormula;
    private String spendPerUseFormulaStatus;
    private String spendPerUseFormulaMessage;
    private boolean allowNegative;
    private String sharedPoolKey;
}
