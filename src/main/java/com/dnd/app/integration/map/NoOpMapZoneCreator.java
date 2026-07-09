package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fallback when the map-service HTTP client is disabled — no live map, so zones are a no-op. */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapZoneCreator implements MapZoneCreator {
    @Override
    public void createZoneForBattle(UUID battleId, ZoneSpec spec) {
        log.debug("map-service integration disabled; not creating a zone for battle {}", battleId);
    }
}
