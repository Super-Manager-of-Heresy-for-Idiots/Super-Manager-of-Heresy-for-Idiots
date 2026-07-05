package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Admin edit of a TRIGGER_REACTION rule (event, timing, predicate, reaction/confirmation flags). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEditRequest {

    private UUID eventTypeId;

    /** {@code before} | {@code after} | {@code replace} | {@code interrupt}. */
    @Size(max = 16)
    private String timing;

    /** DSL boolean predicate gating the trigger; blank = always. */
    @Size(max = 2000)
    private String predicateFormula;

    private boolean requiresPlayerConfirmation;
    private boolean consumesReaction;
}
