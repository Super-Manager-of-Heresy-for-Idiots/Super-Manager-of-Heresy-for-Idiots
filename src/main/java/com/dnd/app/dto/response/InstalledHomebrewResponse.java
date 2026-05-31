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
public class InstalledHomebrewResponse {

    private UUID packageId;
    private String title;
    private String authorUsername;
    private Boolean isDeleted;
    private Instant installedAt;
    private Integer sourceVersion;
    private HomebrewContentSummary contentSummary;
}
