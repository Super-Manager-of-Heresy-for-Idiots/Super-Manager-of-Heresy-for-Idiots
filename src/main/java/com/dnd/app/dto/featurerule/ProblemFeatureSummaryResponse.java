package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One row in the "problem features" list: a class feature that has at least one rule or issue, with
 * aggregate counts so the admin can triage without expanding every row.
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
