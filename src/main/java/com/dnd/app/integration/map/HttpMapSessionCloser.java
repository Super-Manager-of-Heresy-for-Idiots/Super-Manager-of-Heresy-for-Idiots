package com.dnd.app.integration.map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

/**
 * Класс HttpMapSessionCloser описывает интеграционный компонент, который связывает backend с внешним сервисом.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "map-service.http-client-enabled", havingValue = "true")
public class HttpMapSessionCloser implements MapSessionCloser, EnvironmentAware {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param baseUrl входящее значение base url, используемое бизнес-сценарием
     * @param apiKey входящее значение api key, используемое бизнес-сценарием
     */
    public HttpMapSessionCloser(
            @Value("${map-service.base-url:}") String baseUrl,
            @Value("${app.internal.api-key:}") String apiKey
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    /**
     * Устанавливает результат операции "set environment" в рамках бизнес-логики приложения.
     * @param environment входящее значение environment, используемое бизнес-сценарием
     */
    @Override
    public void setEnvironment(Environment environment) {
        boolean localOrTest = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
        if (localOrTest) {
            return;
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new BeanCreationException(
                    "map-service.base-url must be configured when map-service.http-client-enabled=true outside local/test profiles.");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new BeanCreationException(
                    "app.internal.api-key (INTERNAL_API_KEY) must be configured when map-service.http-client-enabled=true outside local/test profiles.");
        }
    }

    /**
     * Выполняет операции "close sessions for battle" в рамках бизнес-логики приложения.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void closeSessionsForBattle(UUID battleId) {
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("map-service.base-url is not configured; skipping map session close for battle {}", battleId);
            return;
        }
        URI uri = URI.create(baseUrl.replaceAll("/+$", "")
                + "/api/internal/sessions/by-battle/" + battleId + "/close");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .header(INTERNAL_API_KEY_HEADER, apiKey)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("map-service session close for battle {} returned HTTP {}", battleId, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while closing map sessions for battle {}", battleId);
        } catch (Exception e) {
            // Soft failure: the battle is already ended; a still-open map is a minor state the GM can close.
            log.warn("Failed to close map sessions for battle {}: {}", battleId, e.getMessage());
        }
    }
}
