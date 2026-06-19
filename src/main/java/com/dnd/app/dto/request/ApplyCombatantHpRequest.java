package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GM manual HP adjustment of a single combatant on the tracker: a negative {@code delta} deals
 * damage, a positive one heals. Used to bookkeep NPCs and correct HP outside the attack flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCombatantHpRequest {

    @NotNull(message = "Delta is required")
    private Integer delta;
}
