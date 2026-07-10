package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ProblemFeatureSummaryResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemFeatureSummaryResponse {
    private UUID featureId;
    private String slug;
    private String title;
    private String className;
    private String subclassName;
    private Integer level;

    private long ruleCount;
    private long approvedRuleCount;
    private long issueCount;
    private long openIssueCount;
    private boolean hasUnresolvedError;
    /** Highest severity among unresolved issues: error > warn > info, or null if none open. */
    private String maxOpenSeverity;
}
