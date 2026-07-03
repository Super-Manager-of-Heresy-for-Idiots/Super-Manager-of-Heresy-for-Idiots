package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of evaluating a DSL expression. Only the field matching {@code resultType} is meaningful. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFormulaEvaluateResponse {
    private boolean ok;
    private String message;
    private String resultType;
    private String displayValue;
    private Double numericValue;
    private Boolean booleanValue;
    private String diceValue;
}
