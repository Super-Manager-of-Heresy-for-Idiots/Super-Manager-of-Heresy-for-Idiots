package com.dnd.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Класс WebPaginationConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 *
 * <p>Ограничивает максимальный размер страницы для всех эндпоинтов, принимающих {@link org.springframework.data.domain.Pageable}.
 * Без этого клиент мог задать {@code size} в тысячи/миллионы записей и заставить сервер материализовать
 * и сериализовать огромную выборку (DoS). Значение с запасом выше любых легитимных запросов UI.</p>
 */
@Configuration
public class WebPaginationConfig {

    /** Верхняя граница размера страницы; клиентский {@code size} обрезается до этого значения. */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * Настраивает резолвер {@link org.springframework.data.domain.Pageable}, ограничивая размер страницы.
     * @return кастомайзер, задающий максимальный размер страницы
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}
