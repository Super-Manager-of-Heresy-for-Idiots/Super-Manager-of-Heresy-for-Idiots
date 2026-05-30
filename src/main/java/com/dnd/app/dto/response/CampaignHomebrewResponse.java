package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignHomebrewResponse {
    private UUID packageId;
    private String title;
    private Integer pinnedVersion;
    private Map<String, Long> contentSummary;
}
