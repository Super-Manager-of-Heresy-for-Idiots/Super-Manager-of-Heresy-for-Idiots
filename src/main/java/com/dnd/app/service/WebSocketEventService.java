package com.dnd.app.service;

import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.WebSocketEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    // NOTE: broadcasts are fire-and-forget on the async (virtual-thread) executor so STOMP
    // serialization/delivery never blocks the request thread. These are pure notifications;
    // clients re-fetch authoritative state over REST. As today, a broadcast may fire slightly
    // before the surrounding transaction commits — for strict ordering, move emission to a
    // @TransactionalEventListener(phase = AFTER_COMMIT). See decisions report.
    @Async
    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, UUID characterId,
                                   Object data, UUID triggeredBy) {
        WebSocketEventPayload payload = WebSocketEventPayload.builder()
                .type(type.name())
                .campaignId(campaignId)
                .characterId(characterId)
                .data(data)
                .timestamp(Instant.now())
                .triggeredBy(triggeredBy)
                .build();

        String destination = "/topic/campaign/" + campaignId;
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("WebSocket event sent: type={}, campaignId={}, destination={}", type, campaignId, destination);
    }

    @Async
    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        // self-invocation runs inline on this async thread (already off the request thread)
        sendCampaignEvent(type, campaignId, null, data, triggeredBy);
    }

    @Async
    public void sendUserEvent(String username, WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        WebSocketEventPayload payload = WebSocketEventPayload.builder()
                .type(type.name())
                .campaignId(campaignId)
                .data(data)
                .timestamp(Instant.now())
                .triggeredBy(triggeredBy)
                .build();

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", payload);
        log.debug("WebSocket user event sent: type={}, user={}", type, username);
    }
}
