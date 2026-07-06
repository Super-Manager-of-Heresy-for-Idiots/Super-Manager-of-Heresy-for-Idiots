package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Named generic formulas attached to a FORMULA rule. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericFormulaRuleAdminResponse {
    private List<FormulaRow> formulas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaRow {
        private UUID id;
        private String formulaKey;
        private String label;
        private String expression;
        private String expressionType;
        private String resultType;
        private String roundingMode;
        private Double minValue;
        private Double maxValue;
        private String validationStatus;
        private String validationMessage;
        private Integer sortOrder;
    }
}
