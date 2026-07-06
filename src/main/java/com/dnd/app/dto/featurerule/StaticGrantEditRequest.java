package com.dnd.app.dto.featurerule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Replace-style edit payload for static proficiency and language grants. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticGrantEditRequest {
    @Valid
    @Size(max = 40)
    private List<ProficiencyGrant> proficiencyGrants;

    @Valid
    @Size(max = 40)
    private List<LanguageGrant> languageGrants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProficiencyGrant {
        @Size(max = 24)
        private String proficiencyType;
        private UUID targetId;
        private UUID filterRuleId;
        private boolean expertise;
        @Size(max = 24)
        private String grantTiming;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageGrant {
        private UUID languageId;
        private UUID filterRuleId;
        @Size(max = 24)
        private String grantTiming;
    }
}
