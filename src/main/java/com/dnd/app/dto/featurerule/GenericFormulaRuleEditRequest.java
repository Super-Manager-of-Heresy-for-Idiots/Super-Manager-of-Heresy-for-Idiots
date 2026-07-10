package com.dnd.app.dto.featurerule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс GenericFormulaRuleEditRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericFormulaRuleEditRequest {
    @Valid
    @Size(max = 40)
    private List<FormulaRow> formulas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaRow {
        @Size(max = 64)
        private String formulaKey;
        @Size(max = 120)
        private String label;
        @Size(max = 2000)
        private String expression;
        @Size(max = 24)
        private String expressionType;
        @Size(max = 16)
        private String resultType;
        @Size(max = 16)
        private String roundingMode;
        private Double minValue;
        private Double maxValue;
        private Integer sortOrder;
    }
}
