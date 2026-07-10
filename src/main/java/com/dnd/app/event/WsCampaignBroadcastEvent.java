package com.dnd.app.event;

import com.dnd.app.dto.response.WebSocketEventPayload;

/**
 * Запись WsCampaignBroadcastEvent описывает событие домена, которое передает изменения бизнес-состояния подписчикам.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param destination входящее значение destination, используемое бизнес-сценарием
 * @param payload входящее значение payload, используемое бизнес-сценарием
 */
public record WsCampaignBroadcastEvent(String destination, WebSocketEventPayload payload) {
}
