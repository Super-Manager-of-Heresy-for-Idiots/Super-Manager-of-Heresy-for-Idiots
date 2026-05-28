package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private String status;
    private Integer version;
    private Integer downloadCount;
    private String authorUsername;
    private List<String> tags;
    private HomebrewContentSummary contentSummary;
    private Map<String, List<ContentSummaryDto>> contentByType;
    private Instant publishedAt;
    private Instant createdAt;
    private Boolean isDeleted;
}
