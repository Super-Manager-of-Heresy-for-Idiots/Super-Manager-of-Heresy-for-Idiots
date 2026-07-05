package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature HEALING rule for the admin editor, with the amount formula's validation status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingRuleAdminResponse {
    private UUID id;
    private String amountFormula;
    /** Result type of the amount formula: {@code integer} (flat pool) or {@code dice} (rolled). */
    private String amountFormulaType;
    private String amountFormulaStatus;
    private String amountFormulaMessage;
    private UUID targetTypeId;
    private boolean tempHp;
    private boolean canReviveFromZero;
}
