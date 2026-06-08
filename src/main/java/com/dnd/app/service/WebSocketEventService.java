package com.dnd.app.service;

import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.WebSocketEventPayload;
import com.dnd.app.event.WsCampaignBroadcastEvent;
import com.dnd.app.event.WsUserBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-to-client notification facade for GAME_MASTER ↔ PLAYER interactions.
 *
 * <p>Callers (GM-triggered service methods) invoke {@link #sendCampaignEvent} /
 * {@link #sendUserEvent} from inside their {@code @Transactional} method. Instead of
 * pushing to the broker immediately, this publishes a Spring application event that is
 * delivered to the STOMP broker only AFTER the transaction commits
 * ({@code WebSocketBroadcastListener}). This avoids notifying clients about state that
 * is later rolled back, while keeping call sites trivial.
 *
 * <p>Payloads are pure notifications — the client re-fetches authoritative state over REST.
 *
 * <ul>
 *   <li>{@code /topic/campaign/{campaignId}} — fan-out to everyone in the campaign (GM + players).</li>
 *   <li>{@code /user/queue/notifications} — a single targeted user (e.g. the kicked player).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventService {

    private final ApplicationEventPublisher eventPublisher;

    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, UUID characterId,
                                   Object data, UUID triggeredBy) {
        if (campaignId == null) {
            return; // characters/quests without a campaign have no audience
        }
        WebSocketEventPayload payload = WebSocketEventPayload.builder()
                .type(type.name())
                .campaignId(campaignId)
                .characterId(characterId)
                .data(data)
                .timestamp(Instant.now())
                .triggeredBy(triggeredBy)
                .build();

        eventPublisher.publishEvent(
                new WsCampaignBroadcastEvent("/topic/campaign/" + campaignId, payload));
        log.debug("WebSocket event queued: type={}, campaignId={}, characterId={}", type, campaignId, characterId);
    }

    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        sendCampaignEvent(type, campaignId, null, data, triggeredBy);
    }

    public void sendUserEvent(String username, WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        if (username == null) {
            return;
        }
        WebSocketEventPayload payload = WebSocketEventPayload.builder()
                .type(type.name())
                .campaignId(campaignId)
                .data(data)
                .timestamp(Instant.now())
                .triggeredBy(triggeredBy)
                .build();

        eventPublisher.publishEvent(
                new WsUserBroadcastEvent(username, "/queue/notifications", payload));
        log.debug("WebSocket user event queued: type={}, user={}", type, username);
    }
}
