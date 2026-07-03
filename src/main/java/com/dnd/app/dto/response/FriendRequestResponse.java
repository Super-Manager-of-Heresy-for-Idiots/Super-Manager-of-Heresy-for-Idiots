package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.FriendRequestDirection;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private UUID relationshipId;
    private UUID userId;
    private String username;
    private String role;
    private FriendRequestDirection direction;
    private Instant createdAt;
}
