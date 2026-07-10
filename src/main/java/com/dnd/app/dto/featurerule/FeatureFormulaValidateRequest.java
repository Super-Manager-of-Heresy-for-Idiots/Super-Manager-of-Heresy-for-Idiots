package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс FeatureFormulaValidateRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFormulaValidateRequest {

    @NotBlank(message = "Выражение обязательно")
    private String expression;

    /** integer | decimal | boolean | duration | dice | modifier. */
    @NotBlank(message = "Тип результата обязателен")
    private String resultType;
}
