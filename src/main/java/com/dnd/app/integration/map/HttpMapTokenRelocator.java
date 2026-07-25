package com.dnd.app.integration.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP-реализация MapTokenRelocator: просит map-сервис перенести токен на целевую карту
 * через внутренний эндпоинт {@code /api/internal/sessions/{sessionId}/tokens/{tokenId}/relocate}
 * (WORLD_PLAN Этап 5). Мягкий отказ: ошибка интеграции только логируется.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "true")
public class HttpMapTokenRelocator implements MapTokenRelocator {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Создаёт HTTP-клиент переноса токенов между картами.
     *
     * @param baseUrl      базовый URL map-сервиса
     * @param apiKey       общий внутренний API-ключ
     * @param objectMapper сериализатор тела запроса
     */
    public HttpMapTokenRelocator(
            @Value("${map-service.base-url:}") String baseUrl,
            @Value("${app.internal.api-key:}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    /**
     * Отправляет map-сервису запрос на перенос токена на целевую карту.
     *
     * @param spec параметры переноса
     * @return true при успешном 2xx-ответе map-сервиса
     */
    @Override
    public boolean relocate(RelocationSpec spec) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("map-service.base-url is not configured; skipping token relocation {}", spec.tokenId());
            return false;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("toMapId", spec.toMapId().toString());
            body.put("toX", spec.toX());
            body.put("toY", spec.toY());

            URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                    + "/api/internal/sessions/" + spec.fromSessionId() + "/tokens/" + spec.tokenId() + "/relocate");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header(INTERNAL_API_KEY_HEADER, apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("map-service token relocation {} returned HTTP {}", spec.tokenId(), response.statusCode());
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while relocating token {}", spec.tokenId());
            return false;
        } catch (Exception e) {
            log.warn("Failed to relocate token {}: {}", spec.tokenId(), e.getMessage());
            return false;
        }
    }
}
