package com.dnd.app.integration.map;

import java.util.UUID;

/**
 * Core → map-service contract: when a battle ends, ask map-service to close any live map sessions
 * linked to it so the tactical map cannot keep running afterwards. Implementations are best-effort —
 * ending the battle must never fail because map-service is unreachable.
 */
@FunctionalInterface
public interface MapSessionCloser {
    void closeSessionsForBattle(UUID battleId);
}
