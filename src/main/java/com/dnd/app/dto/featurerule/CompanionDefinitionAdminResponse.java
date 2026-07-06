package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Rule-authored companion/summon definitions attached to a COMPANION rule. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionDefinitionAdminResponse {
    private List<Companion> companions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Companion {
        private UUID id;
        private String companionKey;
        private UUID monsterId;
        private String nameTemplate;
        private String hpFormula;
        private String hpFormulaStatus;
        private String hpFormulaMessage;
        private String acFormula;
        private String acFormulaStatus;
        private String acFormulaMessage;
        private String attackBonusFormula;
        private String attackBonusFormulaStatus;
        private String attackBonusFormulaMessage;
        private String summonTiming;
        private Integer sortOrder;
        private String notes;
    }
}
