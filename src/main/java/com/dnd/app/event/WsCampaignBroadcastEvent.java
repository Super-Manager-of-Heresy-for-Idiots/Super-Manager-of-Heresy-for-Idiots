package com.dnd.app.event;

import com.dnd.app.dto.response.WebSocketEventPayload;

/**
 * Internal application event: "broadcast this payload to a campaign topic".
 * Published inside a transaction and delivered to the STOMP broker only AFTER the
 * transaction commits (see {@code WebSocketBroadcastListener}), so subscribers never
 * observe an event for state that was later rolled back.
 */
public record WsCampaignBroadcastEvent(String destination, WebSocketEventPayload payload) {
}
