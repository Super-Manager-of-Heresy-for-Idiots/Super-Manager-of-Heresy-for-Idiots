package com.dnd.app.dto.featurerule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Replace-style edit payload for CHOICE rule groups and options. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceRuleEditRequest {
    @Valid
    @Size(max = 20)
    private List<Group> groups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group {
        @Size(max = 64)
        private String choiceKey;
        private Integer minChoices;
        @Size(max = 2000)
        private String maxChoicesFormula;
        @Size(max = 24)
        private String choiceTiming;
        @Size(max = 24)
        private String replacePolicy;
        @Valid
        @Size(max = 80)
        private List<Option> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        @Size(max = 24)
        private String optionType;
        private UUID targetEntityId;
        private UUID filterRuleId;
        private Integer sortOrder;
    }
}
