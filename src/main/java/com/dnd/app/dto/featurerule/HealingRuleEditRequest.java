package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Admin edit of a feature HEALING rule (amount formula + target + temp-HP / revive flags). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingRuleEditRequest {

    /** DSL amount expression: flat (e.g. {@code 5*class_level("paladin")}) or dice (e.g. {@code 1d8+ability_mod("WIS")}); blank = none. */
    @Size(max = 2000)
    private String amountFormula;

    /** {@code integer} for a flat pool or {@code dice} for a rolled amount; defaults to {@code integer}. */
    @Size(max = 16)
    private String amountFormulaType;

    private UUID targetTypeId;
    private boolean tempHp;
    private boolean canReviveFromZero;
}
