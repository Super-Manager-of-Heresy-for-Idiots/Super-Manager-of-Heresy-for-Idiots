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
import java.util.UUID;

/**
 * Класс HttpMapZoneCreator описывает интеграционный компонент, который связывает backend с внешним сервисом.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "true")
public class HttpMapZoneCreator implements MapZoneCreator {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param battle входящее значение battle, используемое бизнес-сценарием
     * @param spec входящее значение spec, используемое бизнес-сценарием
     * @param baseUrl входящее значение base url, используемое бизнес-сценарием
     * @param apiKey входящее значение api key, используемое бизнес-сценарием
     * @param objectMapper входящее значение object mapper, используемое бизнес-сценарием
     */
    public HttpMapZoneCreator(
    /**
     * Создает результат операции "create zone for battle" в рамках бизнес-логики приложения.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param spec входящее значение spec, используемое бизнес-сценарием
     */
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
     * Создает результат операции "create zone for battle" в рамках бизнес-логики приложения.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param spec входящее значение spec, используемое бизнес-сценарием
     */
    @Override
    public void createZoneForBattle(UUID battleId, ZoneSpec spec) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("map-service.base-url is not configured; skipping zone creation for battle {}", battleId);
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("elementType", spec.elementType());
            body.put("originX", spec.originX());
            body.put("originY", spec.originY());
            body.put("sizeFt", spec.sizeFt());
            body.put("rotationDeg", spec.rotationDeg());
            body.put("label", spec.label());
            body.put("terrain", spec.terrain());
            body.put("obscurement", spec.obscurement());
            body.put("sourceCasterCombatantId",
                    spec.sourceCasterCombatantId() != null ? spec.sourceCasterCombatantId().toString() : null);

            URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                    + "/api/internal/sessions/by-battle/" + battleId + "/zones");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header(INTERNAL_API_KEY_HEADER, apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("map-service zone creation for battle {} returned HTTP {}", battleId, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while creating a map zone for battle {}", battleId);
        } catch (Exception e) {
            // Soft failure: the spell is already cast; a missing zone is board state the GM can redraw.
            log.warn("Failed to create a map zone for battle {}: {}", battleId, e.getMessage());
        }
    }
}
