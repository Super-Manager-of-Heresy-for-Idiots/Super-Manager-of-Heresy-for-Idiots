package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Apply a condition (from the bestiary catalogue) to a battle combatant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyConditionRequest {

    @NotNull(message = "conditionId is required")
    private UUID conditionId;

    /** Free-text note on where the condition came from (spell/feature name); optional. */
    private String sourceText;

    /** Duration in rounds; null = lasts until removed. */
    @Min(value = 1, message = "remainingRounds must be at least 1")
    private Integer remainingRounds;
}
