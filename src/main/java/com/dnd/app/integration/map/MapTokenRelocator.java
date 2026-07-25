package com.dnd.app.integration.map;

import java.util.UUID;

/**
 * Контракт MapTokenRelocator связывает core с map-сервисом для переноса токена между
 * картами при проходе через переход (WORLD_PLAN Этап 5): удалить токен на исходной
 * сессии и создать/переместить его на активной сессии целевой карты. Мягкий контракт:
 * ошибка интеграции не отменяет переход (мир — core, доска — map, ГМ может поправить).
 */
public interface MapTokenRelocator {

    /**
     * Переносит токен на целевую карту.
     *
     * @param spec параметры переноса
     * @return выполнен ли перенос на стороне map-сервиса
     */
    boolean relocate(RelocationSpec spec);

    /**
     * Параметры переноса токена между картами.
     *
     * @param fromSessionId сессия исходной карты
     * @param tokenId       токен на исходной карте
     * @param toMapId       целевая карта (map-service definition id)
     * @param toX           клетка появления по X
     * @param toY           клетка появления по Y
     */
    record RelocationSpec(UUID fromSessionId, UUID tokenId, UUID toMapId, int toX, int toY) {
    }
}
