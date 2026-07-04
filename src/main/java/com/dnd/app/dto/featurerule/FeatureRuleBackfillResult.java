package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Summary of a 305-feature backfill run (dry-run or applied). */
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
