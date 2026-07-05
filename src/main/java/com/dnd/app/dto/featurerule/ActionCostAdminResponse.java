package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature ACTION_COST rule for the admin editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCostAdminResponse {
    private UUID id;
    private UUID actionTypeId;
    private Integer amount;
    private String conditionFormula;
    private String conditionFormulaStatus;
    private String conditionFormulaMessage;
}
