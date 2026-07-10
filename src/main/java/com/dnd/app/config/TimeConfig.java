package com.dnd.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Класс TimeConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Configuration
public class TimeConfig {

    /**
     * Выполняет операции "app clock" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    @Bean
    public Clock appClock() {
        return Clock.systemUTC();
    }
}
