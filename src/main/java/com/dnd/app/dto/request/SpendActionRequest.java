package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Marks one of a combatant's action-economy slots as spent for the current turn. Used for the
 * declarative, tabletop-style flow where players/GM announce what they used (action, bonus
 * action or reaction) rather than the server inferring it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendActionRequest {

    @NotNull(message = "Slot is required")
    private Slot slot;

    public enum Slot {
        ACTION, BONUS_ACTION, REACTION
    }
}
