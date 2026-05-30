package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMemberResponse {
    private UUID userId;
    private String username;
    private String roleInCampaign;
    private boolean isCreator;
    private Instant joinedAt;
    private boolean kicked;
}
