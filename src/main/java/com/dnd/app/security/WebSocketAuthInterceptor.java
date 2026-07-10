package com.dnd.app.security;

import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.repository.CampaignMemberRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Класс WebSocketAuthInterceptor описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CampaignMemberRepository campaignMemberRepository;

    /**
     * Выполняет операции "pre send" в рамках бизнес-логики безопасности.
     * @param message входящее значение message, используемое бизнес-сценарием
     * @param channel входящее значение channel, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtTokenProvider.isAccessToken(token)) {
            log.warn("WebSocket CONNECT denied: missing or invalid access token");
            throw new org.springframework.messaging.MessageDeliveryException("Authentication required");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new org.springframework.messaging.MessageDeliveryException("Unknown user"));

        var auth = new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        accessor.setUser(auth);
        log.debug("WebSocket CONNECT authenticated: {}", username);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || accessor.getUser() == null) return;

        String username = accessor.getUser().getName();
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        // Validate campaign subscription membership
        if (destination.startsWith("/topic/campaign.")) {
            try {
                String campaignIdStr = destination.substring("/topic/campaign.".length());
                UUID campaignId = UUID.fromString(campaignIdStr);
                boolean isMember = campaignMemberRepository
                        .existsByCampaignIdAndUserIdAndKickedFalse(campaignId, user.getId());
                if (!isMember && user.getRole() != com.dnd.app.domain.enums.Role.ADMIN) {
                    log.warn("WebSocket SUBSCRIBE denied: user={} not member of campaign={}", username, campaignId);
                    throw new org.springframework.messaging.MessageDeliveryException(
                            "Not authorized to subscribe to this campaign");
                }
            } catch (IllegalArgumentException e) {
                log.warn(
                        "WebSocketAuthInterceptor#handleSubscribe denied: operation=websocket-subscribe-authorize, reason=malformed-campaign-destination, destination={}",
                        destination,
                        e);
                throw new org.springframework.messaging.MessageDeliveryException(
                        "Malformed subscription destination");
            }
        }
    }
}
