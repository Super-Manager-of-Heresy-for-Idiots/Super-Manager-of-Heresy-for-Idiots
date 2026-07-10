package com.dnd.app.event;

import com.dnd.app.dto.response.WebSocketEventPayload;

/**
 * Запись WsUserBroadcastEvent описывает событие домена, которое передает изменения бизнес-состояния подписчикам.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
 * @param destination входящее значение destination, используемое бизнес-сценарием
 * @param payload входящее значение payload, используемое бизнес-сценарием
 */
public record WsUserBroadcastEvent(String username, String destination, WebSocketEventPayload payload) {
}
