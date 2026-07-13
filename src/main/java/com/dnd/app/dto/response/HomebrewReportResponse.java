package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO HomebrewReportResponse — строка админ-очереди жалоб (P2-6): жалоба + пакет и его текущий статус.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewReportResponse {

    private UUID id;
    private UUID packageId;
    private String packageTitle;
    private String packageStatus;
    private String reporterUsername;
    private String reason;
    private String status;
    private Instant createdAt;
    private Instant resolvedAt;
    private String resolvedByUsername;
}
