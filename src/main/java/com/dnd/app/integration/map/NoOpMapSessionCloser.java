package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback used when the map-service HTTP client is disabled ({@code map-service.http-client-enabled}
 * unset or false) — there is no live map to close, so this is a no-op.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapSessionCloser implements MapSessionCloser {
    @Override
    public void closeSessionsForBattle(UUID battleId) {
        log.debug("map-service integration disabled; not closing map sessions for battle {}", battleId);
    }
}
