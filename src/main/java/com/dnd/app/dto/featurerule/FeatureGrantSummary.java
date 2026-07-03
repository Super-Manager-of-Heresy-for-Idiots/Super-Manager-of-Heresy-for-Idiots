package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A proficiency or language grant attached to a feature rule, for the admin card. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureGrantSummary {
    private UUID id;
    private UUID featureRuleId;
    /** "proficiency" or "language". */
    private String kind;
    private String proficiencyType;
    private UUID targetId;
    private UUID languageId;
    private boolean expertise;
    private String grantTiming;
    private UUID filterRuleId;
}
