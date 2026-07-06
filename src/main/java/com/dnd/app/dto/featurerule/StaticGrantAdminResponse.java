package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Static grants attached to a STATIC_GRANT feature rule. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticGrantAdminResponse {
    private List<ProficiencyGrant> proficiencyGrants;
    private List<LanguageGrant> languageGrants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProficiencyGrant {
        private UUID id;
        private String proficiencyType;
        private UUID targetId;
        private UUID filterRuleId;
        private boolean expertise;
        private String grantTiming;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageGrant {
        private UUID id;
        private UUID languageId;
        private UUID filterRuleId;
        private String grantTiming;
    }
}
