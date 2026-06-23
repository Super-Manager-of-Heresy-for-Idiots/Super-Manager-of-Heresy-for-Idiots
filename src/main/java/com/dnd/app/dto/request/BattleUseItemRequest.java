package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The active character consumes one of their carried items in combat (e.g. drinks a healing
 * potion). The item must be a consumable the character owns; one unit is spent. When
 * {@link #targetCombatantId} is omitted the effect applies to the user themselves.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUseItemRequest {

    @NotNull(message = "Item instance ID is required")
    private UUID itemInstanceId;

    /** Optional beneficiary; defaults to the active character (self). */
    private UUID targetCombatantId;
}
