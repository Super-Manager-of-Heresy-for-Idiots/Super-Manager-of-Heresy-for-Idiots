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
 * STOMP / WebSocket configuration with a switchable message broker:
 *
 * <ul>
 *   <li><b>Single pod / dev</b> ({@code app.websocket.relay.enabled=false}, default):
 *       the in-memory {@code SimpleBroker}. Subscriptions live inside the pod, which is fine
 *       when there is exactly one instance.</li>
 *   <li><b>Multi-pod</b> ({@code app.websocket.relay.enabled=true}): an external
 *       {@code StompBrokerRelay} (RabbitMQ with the STOMP plugin). All pods connect to the
 *       shared broker, so a message published on any pod reaches subscribers connected to
 *       any other pod. This is what makes horizontal scaling of WebSocket correct.</li>
 * </ul>
 *
 * <p>For user-targeted destinations ({@code convertAndSendToUser} → {@code /user/queue/...})
 * the relay mode also enables user-destination and user-registry broadcasting, so a message
 * addressed to a user whose session lives on a different pod is routed correctly.
 *
 * <p><b>Role split (running WS as a separate container):</b> {@code app.websocket.endpoint.enabled}
 * controls whether this node terminates client WebSocket connections:
 * <ul>
 *   <li><b>WS role</b> ({@code true}, default): registers the {@code /ws} STOMP endpoint and
 *       accepts client connections. Route {@code /ws} traffic here.</li>
 *   <li><b>REST role</b> ({@code false}): does NOT expose {@code /ws}; it only <em>publishes</em>
 *       events to the broker relay via {@code SimpMessagingTemplate}. The broker (RabbitMQ)
 *       fans them out to whichever WS node holds the subscriber. This lets the REST tier scale
 *       on CPU/DB while the WS tier scales on connection count, independently.</li>
 * </ul>
 * The broker itself is configured on both roles so server-side publishing works everywhere.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.websocket.endpoint.enabled:true}")
    private boolean endpointEnabled;

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

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if (!endpointEnabled) {
            log.info("WebSocket: /ws client endpoint disabled on this node (REST/publish-only role)");
            return;
        }
        String[] origins = allowedOrigins.split("\\s*,\\s*");
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
