package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of validating/committing a combatant's movement spend against their per-turn budget.
 *
 * <p>This is an explicit allowed-flag envelope (always HTTP 200) rather than a 4xx on refusal, so
 * the caller (map-service) receives the {@code reason} and remaining budget even when the move is
 * rejected — it needs both to snap the token back and show a precise message. When {@code allowed}
 * is false the spend was NOT committed; when true the budget was incremented by the requested feet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResultResponse {

    /** Whether the spend was committed (true) or refused with no state change (false). */
    private boolean allowed;

    /** null when allowed; otherwise NOT_ACTIVE_TURN | MOVEMENT_BUDGET_EXCEEDED | BATTLE_NOT_ACTIVE. */
    private String reason;

    /** Feet of movement left this turn after this call; may be negative under a GM override. */
    private int remainingFt;

    /** The combatant's total movement speed this turn, in feet. */
    private int speedFt;

    /** True if the requested feet fit within the budget on an active turn, regardless of override. */
    private boolean withinBudget;

    /** True if checks were skipped because a GM explicitly overrode the rules (logged by map). */
    private boolean gmOverride;
}
