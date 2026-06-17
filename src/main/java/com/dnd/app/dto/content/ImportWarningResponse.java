package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A content-import warning surfaced to admins.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ImportWarning", description = "A non-fatal content import warning")
public class ImportWarningResponse {
    private UUID id;
    private String sourceSlug;
    private String entityKind;
    private String entitySlug;
    private String warningCode;
    private String message;
    private Instant createdAt;
}
