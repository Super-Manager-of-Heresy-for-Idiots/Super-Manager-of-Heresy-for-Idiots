package com.dnd.app.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service-to-service relationship projection consumed by the messenger service
 * ({@code GET /api/internal/users/{userId}/relationships/{otherUserId}}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalRelationshipResponse {
    private boolean friends;
    private boolean blocked;
    private List<UserSummaryResponse> userSummaries;
}
