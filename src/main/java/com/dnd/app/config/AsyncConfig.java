package com.dnd.app.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс AsyncConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Исполнитель асинхронных задач контроллеров: виртуальные нити с распространением SecurityContext.
     *
     * <p>N9: при {@code app.async.enabled=true} (дефолт) вокруг исполнителя виртуальных нитей
     * оборачивается {@link BoundedExecutorService} — admission control, ограничивающий число запросов
     * «в полёте» (kill-switch {@code false} возвращает ровно прежнее безлимитное поведение). Обёртка
     * стоит ВНУТРИ {@link DelegatingSecurityContextExecutorService}, чтобы контекст безопасности
     * по-прежнему распространялся снаружи, как раньше.
     *
     * @param enabled включён ли admission control (false = прежнее безлимитное поведение)
     * @param maxInFlight максимум задач «в полёте» на под
     * @param acquireTimeoutMs ожидание свободного слота перед 503, миллисекунды
     * @param meterRegistry реестр метрик Micrometer (in-flight gauge, rejected counter)
     * @return исполнитель задач контроллеров
     */
    @Bean(destroyMethod = "shutdown")
    public Executor controllerTaskExecutor(
            @Value("${app.async.enabled:true}") boolean enabled,
            @Value("${app.async.max-in-flight:200}") int maxInFlight,
            @Value("${app.async.acquire-timeout-ms:2000}") long acquireTimeoutMs,
            MeterRegistry meterRegistry) {
        ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService inner = enabled
                ? new BoundedExecutorService(virtualThreads, maxInFlight, acquireTimeoutMs, meterRegistry)
                : virtualThreads;
        return new DelegatingSecurityContextExecutorService(inner);
    }
}
