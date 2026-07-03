package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** A choice group (with its options) attached to a feature rule, for the admin card. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureChoiceSummary {
    private UUID id;
    private UUID featureRuleId;
    private String choiceKey;
    private Integer minChoices;
    private UUID maxChoicesFormulaId;
    private String choiceTiming;
    private String replacePolicy;
    private List<Option> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private UUID id;
        private String optionType;
        private UUID targetEntityId;
        private UUID filterRuleId;
        private Integer sortOrder;
    }
}
