package com.dnd.app.service;

import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.WebSocketEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

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

    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        sendCampaignEvent(type, campaignId, null, data, triggeredBy);
    }

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
