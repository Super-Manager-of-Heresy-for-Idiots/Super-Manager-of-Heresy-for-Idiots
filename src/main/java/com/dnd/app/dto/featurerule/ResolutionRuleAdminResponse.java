package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature SAVE_CHECK_ATTACK resolution rule for the admin editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionRuleAdminResponse {
    private UUID id;
    private String resolutionType;
    private UUID abilityId;
    private UUID skillId;
    private String dcFormula;
    private String dcFormulaStatus;
    private String dcFormulaMessage;
}
