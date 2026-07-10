package com.dnd.app.config;

import com.dnd.app.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Класс WebSocketConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Выполняет операции "configure message broker" в рамках бизнес-логики инфраструктуры.
     * @param config входящее значение config, используемое бизнес-сценарием
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.websocket.relay.enabled:false}")
    private boolean relayEnabled;
    @Value("${app.websocket.relay.host:localhost}")
    private String relayHost;
    @Value("${app.websocket.relay.port:61613}")
    private int relayPort;
    @Value("${app.websocket.relay.login:guest}")
    private String relayLogin;
    @Value("${app.websocket.relay.passcode:guest}")
    private String relayPasscode;
    @Value("${app.websocket.relay.virtual-host:/}")
    private String relayVirtualHost;

    /**
     * Выполняет операции "configure message broker" в рамках бизнес-логики инфраструктуры.
     * @param config входящее значение config, используемое бизнес-сценарием
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (relayEnabled) {
            log.info("WebSocket: using STOMP broker relay at {}:{} (multi-pod mode)", relayHost, relayPort);
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayLogin)
                    .setClientPasscode(relayPasscode)
                    .setSystemLogin(relayLogin)
                    .setSystemPasscode(relayPasscode)
                    .setVirtualHost(relayVirtualHost)
                    // Required for cross-pod user-targeted messaging:
                    .setUserDestinationBroadcast("/topic/unresolved-user-destination")
                    .setUserRegistryBroadcast("/topic/simp-user-registry");
        } else {
            log.info("WebSocket: using in-memory SimpleBroker (single-pod mode)");
            config.enableSimpleBroker("/topic", "/queue");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Выполняет операции "register stomp endpoints" в рамках бизнес-логики инфраструктуры.
     * @param registry входящее значение registry, используемое бизнес-сценарием
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The /ws endpoint is registered on EVERY node: @EnableWebSocketMessageBroker requires at
        // least one STOMP endpoint, otherwise subProtocolWebSocketHandler fails to start ("No handlers").
        // The REST/WS role split is enforced by routing (nginx sends /ws only to the WS tier), not by
        // withholding the endpoint here — the REST tier simply never receives /ws traffic.
        String[] origins = allowedOrigins.split("\\s*,\\s*");
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    /**
     * Выполняет операции "configure client inbound channel" в рамках бизнес-логики инфраструктуры.
     * @param registration входящее значение registration, используемое бизнес-сценарием
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
