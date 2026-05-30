package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamHomebrewActivationResponse {
    private UUID homebrewPackageId;
    private String title;
    private Map<String, Long> contentSummary;
    private Instant activatedAt;
}
