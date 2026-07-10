package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс FeatureFormulaValidationResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFormulaValidationResponse {
    private boolean valid;
    private String message;
    /** Context variables/functions the expression needs (e.g. class_level(Druid), proficiency_bonus). */
    private List<String> requiredContext;
    /** Result computed against a probe context, for a quick sanity display. */
    private String sampleResult;
}
