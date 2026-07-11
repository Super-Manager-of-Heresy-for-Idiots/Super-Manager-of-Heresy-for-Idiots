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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP-реализация MapTokenMover: шлёт map-сервису принудительное перемещение токенов через внутренний
 * эндпоинт {@code /api/internal/sessions/by-battle/{battleId}/forced-move} (фаза 2.12). Активна, когда
 * включён {@code map-service.http-client-enabled}. Мягкий отказ: перемещение — состояние доски, которое
 * GM может поправить вручную, поэтому ошибка интеграции только логируется.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "true")
public class HttpMapTokenMover implements MapTokenMover {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Создаёт HTTP-клиент перемещения токенов.
     *
     * @param baseUrl      базовый URL map-сервиса
     * @param apiKey       общий внутренний API-ключ
     * @param objectMapper сериализатор тела запроса
     */
    public HttpMapTokenMover(
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
     * Отправляет map-сервису запрос на принудительное перемещение токенов боя.
     *
     * @param battleId идентификатор боя
     * @param spec     тип перемещения и список перемещений токенов
     */
    @Override
    public void forcedMove(UUID battleId, ForcedMoveSpec spec) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("map-service.base-url is not configured; skipping forced move for battle {}", battleId);
            return;
        }
        try {
            List<Map<String, Object>> moves = new ArrayList<>();
            for (TokenMove mv : spec.moves()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("externalCombatantId", mv.combatantId().toString());
                m.put("toX", mv.toX());
                m.put("toY", mv.toY());
                moves.add(m);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("movementType", spec.movementType());
            body.put("moves", moves);

            URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                    + "/api/internal/sessions/by-battle/" + battleId + "/forced-move");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header(INTERNAL_API_KEY_HEADER, apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("map-service forced move for battle {} returned HTTP {}", battleId, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while forcing a token move for battle {}", battleId);
        } catch (Exception e) {
            log.warn("Failed to force a token move for battle {}: {}", battleId, e.getMessage());
        }
    }
}
