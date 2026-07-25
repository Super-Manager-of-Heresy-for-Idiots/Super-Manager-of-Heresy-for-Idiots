package com.dnd.app.integration.map;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP-реализация MapTokenReader: читает токен через внутренний эндпоинт map-сервиса
 * {@code /api/internal/sessions/{sessionId}/tokens/{tokenId}} (WORLD_PLAN Этап 5).
 * Мягкий отказ: любая ошибка интеграции трактуется как "позиция неизвестна".
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "true")
public class HttpMapTokenReader implements MapTokenReader {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Создаёт HTTP-клиент чтения токенов.
     *
     * @param baseUrl      базовый URL map-сервиса
     * @param apiKey       общий внутренний API-ключ
     * @param objectMapper парсер тела ответа
     */
    public HttpMapTokenReader(
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
     * Читает позицию токена из map-сервиса; при недоступности возвращает empty.
     *
     * @param sessionId идентификатор сессии карты
     * @param tokenId   идентификатор токена
     * @return позиция токена или empty
     */
    @Override
    public Optional<TokenPosition> getTokenPosition(UUID sessionId, UUID tokenId) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("map-service.base-url is not configured; token position unknown for token {}", tokenId);
            return Optional.empty();
        }
        try {
            URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                    + "/api/internal/sessions/" + sessionId + "/tokens/" + tokenId);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .header(INTERNAL_API_KEY_HEADER, apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("map-service token read {} returned HTTP {}", tokenId, response.statusCode());
                return Optional.empty();
            }
            JsonNode node = objectMapper.readTree(response.body());
            JsonNode gridX = node.path("gridX");
            JsonNode gridY = node.path("gridY");
            if (gridX.isMissingNode() || gridY.isMissingNode()) {
                return Optional.empty();
            }
            UUID characterId = node.hasNonNull("characterId")
                    ? UUID.fromString(node.get("characterId").asText())
                    : null;
            return Optional.of(new TokenPosition((int) Math.floor(gridX.asDouble()),
                    (int) Math.floor(gridY.asDouble()), characterId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while reading token {} position", tokenId);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to read token {} position: {}", tokenId, e.getMessage());
            return Optional.empty();
        }
    }
}
