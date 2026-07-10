package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureRuleDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleDetailResponse {
    private UUID featureId;
    private String slug;
    private String title;
    private String className;
    private String subclassName;
    private Integer level;
    private String description;
    private List<FeatureRuleResponse> rules;
    private List<FeatureRuleIssueResponse> issues;
    /** Static proficiency/language grants authored on this feature's rules (Stage 4). */
    private List<FeatureGrantSummary> grants;
    /** Choice groups authored on this feature's rules (Stage 4). */
    private List<FeatureChoiceSummary> choices;
}
