package com.dnd.app.event;

import com.dnd.app.dto.response.WebSocketEventPayload;

/**
 * Internal application event: "deliver this payload to a single user's queue".
 * {@code username} is the STOMP principal name (the account username, set by
 * {@code WebSocketAuthInterceptor}). Delivered after transaction commit.
 */
public record WsUserBroadcastEvent(String username, String destination, WebSocketEventPayload payload) {
}
