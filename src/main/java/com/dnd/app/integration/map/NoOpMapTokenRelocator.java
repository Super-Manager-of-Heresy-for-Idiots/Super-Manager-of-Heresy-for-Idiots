package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка MapTokenRelocator при отключённой интеграции с map-сервисом (WORLD_PLAN Этап 5):
 * токен не переносится, переход выполняется только на уровне мира (локации).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapTokenRelocator implements MapTokenRelocator {

    /**
     * Ничего не переносит — интеграция с map-сервисом отключена.
     *
     * @param spec параметры переноса (игнорируются)
     * @return всегда false
     */
    @Override
    public boolean relocate(RelocationSpec spec) {
        log.debug("map-service integration disabled; not relocating token {}", spec.tokenId());
        return false;
    }
}
