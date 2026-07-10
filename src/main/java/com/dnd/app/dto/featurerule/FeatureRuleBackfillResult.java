package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс FeatureRuleBackfillResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleBackfillResult {
    private boolean applied;
    private int runtimeFeatures;
    private int featuresTouched;
    private int featuresSkipped;
    private int rulesCreated;
    private int issuesCreated;
    private int formulasCreated;
}
