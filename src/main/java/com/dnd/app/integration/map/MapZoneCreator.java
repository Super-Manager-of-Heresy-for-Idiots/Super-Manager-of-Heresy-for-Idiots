package com.dnd.app.integration.map;

import java.util.UUID;

/**
 * Контракт MapZoneCreator описывает интеграционный компонент, который связывает backend с внешним сервисом.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
