package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Service-to-service answer to "what may this user do in this campaign?".
 * Consumed by map-service for campaign map and map asset authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAccessResponse {

    private UUID campaignId;
    private UUID userId;
    private boolean canView;
    private boolean canManageMaps;
    private boolean canMoveAnyToken;
    private List<UUID> movableCharacterIds;
}
