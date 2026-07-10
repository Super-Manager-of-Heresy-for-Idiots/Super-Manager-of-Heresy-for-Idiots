package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс FeatureFormulaEvaluateRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFormulaEvaluateRequest {

    @NotBlank(message = "Выражение обязательно")
    private String expression;

    /** integer | decimal | boolean | duration | dice | modifier. */
    @NotBlank(message = "Тип результата обязателен")
    private String resultType;

    /** floor | ceil | nearest | none (applied to numeric results). */
    private String roundingMode;

    private Double minValue;
    private Double maxValue;

    private FormulaContextPayload context;
}
