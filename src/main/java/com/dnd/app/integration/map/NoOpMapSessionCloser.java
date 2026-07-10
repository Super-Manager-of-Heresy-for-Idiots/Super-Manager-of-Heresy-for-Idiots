package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс NoOpMapSessionCloser описывает интеграционный компонент, который связывает backend с внешним сервисом.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMapSessionCloser implements MapSessionCloser {
    @Override
    /**
     * Выполняет операции "close sessions for battle" в рамках бизнес-логики приложения.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     */
    public void closeSessionsForBattle(UUID battleId) {
        log.debug("map-service integration disabled; not closing map sessions for battle {}", battleId);
    }
}
