package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Result of saving (create/update) a class via the authoring contract. Returns the full
 * saved graph in read form plus metadata for toast/close/cache-invalidation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassSaveResult", description = "Result of an aggregate class create/update")
public class ClassSaveResult {

    @Schema(description = "Canonical read model with new ids of all children")
    private ContentClassDetailResponse clazz;

    private UUID id;
    private String slug;

    @Schema(description = "Homebrew package id; null for admin/core")
    private UUID packageId;

    @Schema(description = "ETag/version for If-Match on the next update")
    private String etag;

    private String createdAt;
    private String updatedAt;

    @Schema(description = "Accepted non-fatal warnings")
    private List<ValidationIssue> warnings;

    @Schema(description = "Canonical GET URL")
    private String resourceUrl;
}
