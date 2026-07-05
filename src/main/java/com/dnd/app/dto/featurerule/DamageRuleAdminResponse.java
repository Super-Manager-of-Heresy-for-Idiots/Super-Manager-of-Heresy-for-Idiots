package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature DAMAGE rule for the admin editor, with the formulas' validation status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamageRuleAdminResponse {
    private UUID id;
    private String diceFormula;
    private String diceFormulaStatus;
    private String diceFormulaMessage;
    private String flatFormula;
    private String flatFormulaStatus;
    private String flatFormulaMessage;
    private UUID damageTypeId;
    private boolean requiresAttackHit;
    private boolean requiresSave;
    private boolean halfOnSave;
}
