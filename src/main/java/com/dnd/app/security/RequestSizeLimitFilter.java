package com.dnd.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Класс RequestSizeLimitFilter описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 *
 * <p>Backstop-ограничение размера тела запроса. Отклоняет запросы, у которых {@code Content-Length}
 * превышает лимит, ещё до чтения тела и десериализации Jackson — защита от исчерпания памяти большими
 * JSON-телами (у {@code application/json} нет иного серверного лимита). В проде основной барьер держит
 * nginx ({@code client_max_body_size}); это дополнительный уровень на стороне приложения. Запросы без
 * {@code Content-Length} (chunked transfer-encoding) здесь не проверяются и ограничиваются nginx/контейнером.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    /**
     * Создает экземпляр компонента безопасности и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param maxBytes максимально допустимый размер тела запроса в байтах (по умолчанию 25 МиБ)
     */
    public RequestSizeLimitFilter(@Value("${app.security.max-request-bytes:26214400}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (maxBytes > 0 && length > maxBytes) {
            log.warn("Request rejected: contentLength={} exceeds limit={} for {} {}",
                    length, maxBytes, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Request body too large\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
