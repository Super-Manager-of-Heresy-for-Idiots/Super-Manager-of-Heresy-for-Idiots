package com.dnd.app.integration.map;

import java.util.UUID;

/**
 * Core → map: materialize a lingering spell zone (Web etc., Phase 2.3) on the battle's live map
 * sessions. Best-effort like {@link MapSessionCloser}: the cast itself never depends on map-service
 * being up — a missing zone is a board-state gap the GM can recreate, not a failed cast.
 */
public interface MapZoneCreator {

    /** @param spec everything map needs to draw + apply the zone (shape/size/origin/terrain/...). */
    void createZoneForBattle(UUID battleId, ZoneSpec spec);

    record ZoneSpec(
            String elementType,
            int originX,
            int originY,
            int sizeFt,
            double rotationDeg,
            String label,
            String terrain,
            String obscurement,
            UUID sourceCasterCombatantId
    ) {
    }
}
