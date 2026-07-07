package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Read-only movement snapshot for a battle, so map-service / the tactical UI can preview how far the
 * active combatant may still move (and show every combatant's speed) without recomputing speed from
 * the character sheet. The authoritative source of speed and spent budget is core BE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementContextResponse {

    /** The combatant whose turn it is, or null if the battle is not active. */
    private UUID activeCombatantId;

    private int roundNumber;

    private List<CombatantMovement> combatants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombatantMovement {
        private UUID combatantId;
        private int speedFt;
        private int movementUsedFt;
        private int remainingFt;
    }
}
