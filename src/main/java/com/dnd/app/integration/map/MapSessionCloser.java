package com.dnd.app.integration.map;

import java.util.UUID;

/**
 * Контракт MapSessionCloser описывает интеграционный компонент, который связывает backend с внешним сервисом.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@FunctionalInterface
public interface MapSessionCloser {
    void closeSessionsForBattle(UUID battleId);
}
