package com.dnd.app.service;

import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.WebSocketEventPayload;
import com.dnd.app.event.WsCampaignBroadcastEvent;
import com.dnd.app.event.WsUserBroadcastEvent;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс WebSocketEventService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketEventService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    /**
     * Публикует событие операции "send campaign event" в рамках бизнес-логики домена.
     * @param type входящее значение type, используемое бизнес-сценарием
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param data входящее значение data, используемое бизнес-сценарием
     * @param triggeredBy входящее значение triggered by, используемое бизнес-сценарием
     */
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
                .triggeredByName(resolveTriggeredByName(triggeredBy))
                .build();

        eventPublisher.publishEvent(
                new WsCampaignBroadcastEvent("/topic/campaign." + campaignId, payload));
        log.debug("WebSocket event queued: type={}, campaignId={}, characterId={}", type, campaignId, characterId);
    }

    /**
     * Публикует событие операции "send campaign event" в рамках бизнес-логики домена.
     * @param type входящее значение type, используемое бизнес-сценарием
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param data входящее значение data, используемое бизнес-сценарием
     * @param triggeredBy входящее значение triggered by, используемое бизнес-сценарием
     */
    public void sendCampaignEvent(WebSocketEventType type, UUID campaignId, Object data, UUID triggeredBy) {
        sendCampaignEvent(type, campaignId, null, data, triggeredBy);
    }

    /**
     * Публикует событие операции "send user event" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param type входящее значение type, используемое бизнес-сценарием
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param data входящее значение data, используемое бизнес-сценарием
     * @param triggeredBy входящее значение triggered by, используемое бизнес-сценарием
     */
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
                .triggeredByName(resolveTriggeredByName(triggeredBy))
                .build();

        eventPublisher.publishEvent(
                new WsUserBroadcastEvent(username, "/queue/notifications", payload));
        log.debug("WebSocket user event queued: type={}, user={}", type, username);
    }

    private String resolveTriggeredByName(UUID triggeredBy) {
        if (triggeredBy == null) {
            return null;
        }
        return userRepository.findById(triggeredBy)
                .map(user -> user.getUsername())
                .orElse(null);
    }
}
