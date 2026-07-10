package com.dnd.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Класс LegacyContentRouteLoggingConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Configuration
public class LegacyContentRouteLoggingConfig implements WebMvcConfigurer {

    /**
     * Still-active legacy routes tied to runtime data, kept until the data
     * migration runs in prod. Stable routes now point at the content model; only
     * explicit /legacy paths should warn.
     */
    private static final List<String> LEGACY_PATTERNS = List.of(
            "/api/characters/*/legacy/level-up-options",
            "/api/characters/*/legacy/level-up",
            "/api/characters/legacy/full",
            "/api/campaigns/*/characters/legacy/full");

            /**
             * Выполняет операции "pre handle" в рамках бизнес-логики инфраструктуры.
             * @param request входящие данные запроса для выполнения бизнес-сценария
             * @param response входящее значение response, используемое бизнес-сценарием
             * @param handler входящее значение handler, используемое бизнес-сценарием
    /**
     * Добавляет результат операции "add interceptors" в рамках бизнес-логики инфраструктуры.
     * @param registry входящее значение registry, используемое бизнес-сценарием
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            /**
             * Выполняет операции "pre handle" в рамках бизнес-логики инфраструктуры.
             * @param request входящие данные запроса для выполнения бизнес-сценария
             * @param response входящее значение response, используемое бизнес-сценарием
             * @param handler входящее значение handler, используемое бизнес-сценарием
             * @return результат выполнения бизнес-операции
             */
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                log.warn("LEGACY content route hit (superseded by new content model): {} {}",
                        request.getMethod(), request.getRequestURI());
                return true;
            }
        }).addPathPatterns(LEGACY_PATTERNS);
    }
}
