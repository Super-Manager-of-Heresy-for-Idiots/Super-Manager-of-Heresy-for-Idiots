package com.dnd.app.integration.map;

import java.util.Optional;
import java.util.UUID;

/**
 * Контракт MapTokenReader связывает core с map-сервисом для чтения позиции токена
 * (WORLD_PLAN Этап 5): валидация "токен действительно стоит на ключевой клетке перехода".
 * Мягкий контракт: при недоступности map-сервиса позиция считается неизвестной и
 * серверная проверка позиции пропускается (мир — ответственность core, доска — map).
 */
public interface MapTokenReader {

    /**
     * Возвращает позицию токена в сессии карты, если map-сервис доступен и токен найден.
     *
     * @param sessionId идентификатор сессии карты
     * @param tokenId   идентификатор токена
     * @return позиция токена или {@link Optional#empty()}, если недоступно
     */
    Optional<TokenPosition> getTokenPosition(UUID sessionId, UUID tokenId);

    /**
     * Позиция токена на сетке.
     *
     * @param gridX       клетка по X (якорь токена)
     * @param gridY       клетка по Y (якорь токена)
     * @param characterId персонаж, к которому привязан токен (или null)
     */
    record TokenPosition(int gridX, int gridY, UUID characterId) {
    }
}
