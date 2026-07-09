package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cast a spell inside the battle flow (Phase 2.1). The caster is always the combatant whose turn it
 * is (the server does not take a casterId from the client). {@code targetCombatantId} is optional
 * (self/no-target spells); {@code slotLevel} upcasts (>= spell level). {@code clientCommandId} is the
 * idempotency key (A6; dedup infrastructure lands in 2.14, the field is accepted now).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleCastSpellRequest {

    @NotNull(message = "spellId is required")
    private UUID spellId;

    private UUID targetCombatantId;

    @Min(value = 0, message = "slotLevel must be 0-9")
    private Integer slotLevel;

    private UUID clientCommandId;
}
