package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Заглушка MapTokenMover на случай отключённой интеграции с map-сервисом (фаза 2.12): ничего не делает,
 * только логирует. Активна, когда {@code map-service.http-client-enabled} не {@code true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapTokenMover implements MapTokenMover {

    /**
     * Ничего не перемещает — интеграция с map-сервисом отключена.
     *
     * @param battleId идентификатор боя
     * @param spec     спецификация перемещения (игнорируется)
     */
    @Override
    public void forcedMove(UUID battleId, ForcedMoveSpec spec) {
        log.debug("map-service integration disabled; not moving tokens for battle {}", battleId);
    }
}
