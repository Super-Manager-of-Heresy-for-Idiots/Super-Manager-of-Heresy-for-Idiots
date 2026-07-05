package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A TRIGGER_REACTION binding for the admin editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerAdminResponse {
    private UUID id;
    private UUID eventTypeId;
    private String timing;
    private String predicateFormula;
    private String predicateFormulaStatus;
    private String predicateFormulaMessage;
    private boolean requiresPlayerConfirmation;
    private boolean consumesReaction;
}
