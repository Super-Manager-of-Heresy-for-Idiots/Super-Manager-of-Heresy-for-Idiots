package com.dnd.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Класс MessengerClient описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
public class MessengerClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final boolean enabled;
    private final String baseUrl;
    private final String internalApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "messenger-client");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param enabled входящее значение enabled, используемое бизнес-сценарием
     * @param a входящее значение a, используемое бизнес-сценарием
     * @param b входящее значение b, используемое бизнес-сценарием
     * @param reason входящее значение reason, используемое бизнес-сценарием
     */
    public MessengerClient(
            @Value("${app.messenger.http-client-enabled:false}") boolean enabled,
            @Value("${app.messenger.base-url:http://localhost:8082}") String baseUrl,
            @Value("${app.internal.api-key:}") String internalApiKey,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    /**
     * Выполняет операции "close session for pair" в рамках бизнес-логики домена.
     * @param userAId идентификатор user a, используемый для выбора нужного бизнес-объекта
     * @param userBId идентификатор user b, используемый для выбора нужного бизнес-объекта
     * @param reason входящее значение reason, используемое бизнес-сценарием
     */
    public void closeSessionForPair(UUID userAId, UUID userBId, String reason) {
        if (!enabled) {
            return;
        }
        executor.submit(() -> sendWithRetry(userAId, userBId, reason));
    }

    private void sendWithRetry(UUID userAId, UUID userBId, String reason) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                send(userAId, userBId, reason);
                return;
            } catch (Exception exception) {
                log.warn("close-by-pair attempt {} to messenger failed: {}", attempt, exception.toString());
            }
        }
        log.warn("Giving up on close-by-pair for pair after retries; relationship cache is the fallback.");
    }

    private void send(UUID userAId, UUID userBId, String reason) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "userAId", userAId,
                "userBId", userBId,
                "reason", reason));
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/api/internal/chat-sessions/close-by-pair"))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (StringUtils.hasText(internalApiKey)) {
            builder.header(INTERNAL_API_KEY_HEADER, internalApiKey);
        }
        HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("messenger returned status " + response.statusCode());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
