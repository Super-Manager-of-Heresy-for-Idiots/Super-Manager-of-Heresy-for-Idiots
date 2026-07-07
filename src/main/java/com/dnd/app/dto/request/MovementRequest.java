package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Internal (service-to-service) request from map-service to spend a combatant's movement budget for
 * the current turn. The spatial cost of the path is computed authoritatively in map-service and sent
 * here as {@code feet}; core validates it against the combatant's per-turn budget and commits it.
 *
 * <p>{@code gmOverride} is passed only for a GM who explicitly moved a token outside the rules; core
 * then skips the turn/budget checks, still commits the spend and flags the result as overridden.
 * {@code clientCommandId} is the idempotency key — the dedup infrastructure lands in Phase 2.14, the
 * field is carried now (A6) so the contract is stable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementRequest {

    @NotNull(message = "combatantId is required")
    private UUID combatantId;

    @NotNull(message = "feet is required")
    @PositiveOrZero(message = "feet must be zero or positive")
    private Integer feet;

    /** GM explicitly moving a token outside the rules; skips checks but is flagged in the result. */
    private boolean gmOverride;

    /** Idempotency key for the move command (dedup wired in Phase 2.14). */
    private UUID clientCommandId;
}
