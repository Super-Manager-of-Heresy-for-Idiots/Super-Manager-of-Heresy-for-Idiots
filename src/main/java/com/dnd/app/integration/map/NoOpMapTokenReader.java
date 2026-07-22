package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Заглушка MapTokenReader при отключённой интеграции с map-сервисом (WORLD_PLAN Этап 5):
 * позиция токена всегда неизвестна, серверная валидация позиции пропускается.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapTokenReader implements MapTokenReader {

    /**
     * Возвращает empty — интеграция с map-сервисом отключена.
     *
     * @param sessionId идентификатор сессии карты
     * @param tokenId   идентификатор токена
     * @return всегда {@link Optional#empty()}
     */
    @Override
    public Optional<TokenPosition> getTokenPosition(UUID sessionId, UUID tokenId) {
        log.debug("map-service integration disabled; token {} position unknown", tokenId);
        return Optional.empty();
    }
}
