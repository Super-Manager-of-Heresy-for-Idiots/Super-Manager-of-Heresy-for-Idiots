package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal, safe identity of a single battle combatant for map-service to create a token-combat
 * link from. Deliberately excludes private character-sheet data. {@code widthCells}/{@code
 * heightCells} default to a 1x1 footprint — token sizing is a spatial concern owned by
 * map-service, core BE only states the default.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantReferenceResponse {

    private UUID battleId;
    private UUID campaignId;
    private UUID combatantId;
    private String type;
    private String displayName;
    private UUID characterId;
    private UUID monsterId;
    private UUID ownerUserId;
    private Integer currentHp;
    private Integer maxHp;
    private Integer turnOrder;
    private boolean currentTurn;
    private int widthCells;
    private int heightCells;
}
