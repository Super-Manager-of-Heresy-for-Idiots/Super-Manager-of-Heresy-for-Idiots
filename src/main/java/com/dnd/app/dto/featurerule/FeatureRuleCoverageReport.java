package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Класс FeatureRuleCoverageReport описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleCoverageReport {
    private int runtimeFeatures;
    private int featuresWithRules;
    private int featuresWithApprovedRules;
    private int featuresWithoutRules;
    private int featuresWithUnresolvedError;
    private long totalRules;
    private long approvedRules;
    private long needsReviewRules;
    /** rule_type code -> count. */
    private Map<String, Long> rulesByType;
    /** review_status code -> count. */
    private Map<String, Long> rulesByStatus;
    /** class name -> count of features that have rules. */
    private Map<String, Long> coverageByClass;
}
