package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс FeatureFormulaEvaluateResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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
