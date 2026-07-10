package com.dnd.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Класс AsyncConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Выполняет операции "controller task executor" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    @Bean(destroyMethod = "shutdown")
    public Executor controllerTaskExecutor() {
        return new DelegatingSecurityContextExecutorService(
                Executors.newVirtualThreadPerTaskExecutor());
    }
}
