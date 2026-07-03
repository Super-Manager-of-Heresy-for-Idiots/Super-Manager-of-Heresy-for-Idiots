package com.dnd.app.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for FRIEND_* WebSocket notifications (the client re-fetches authoritative state over REST). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendNotificationData {
    private UUID relationshipId;
    private UUID userId;
    private String username;
}
