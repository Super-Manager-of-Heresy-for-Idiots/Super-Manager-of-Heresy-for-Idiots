package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * The full "feature card" for the Rule Workbench: the source description plus all rules and issues of
 * a single class feature. Loaded lazily when a feature row is expanded.
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
