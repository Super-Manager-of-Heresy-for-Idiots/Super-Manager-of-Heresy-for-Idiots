package com.dnd.app.dto.featurerule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * A feature choice a character must make (Fighting Style, Expertise skills, Metamagic, …) with its options
 * and the character's current selections. Drives the level-up / folio pending-choices UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeatureChoiceGroup", description = "A feature choice group with options and current selections")
public class FeatureChoiceGroupResponse {

    private UUID groupId;
    private UUID featureId;
    private String choiceKey;
    private int minChoices;
    private int maxChoices;
    private int chosenCount;
    private int remaining;
    private List<Option> options;
    private List<Selection> selections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private UUID id;
        private String optionType;
        private UUID targetEntityId;
        private UUID filterRuleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Selection {
        private UUID id;
        private String optionType;
        private UUID targetEntityId;
        private Integer chosenAtLevel;
    }
}
