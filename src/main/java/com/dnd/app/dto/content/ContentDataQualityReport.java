package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ContentDataQualityReport описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentDataQualityReport", description = "Data-quality findings for the new content model")
public class ContentDataQualityReport {

    @Schema(description = "Core class features not referenced by any reward group or FEATURE grant")
    private List<FeatureGap> featuresWithoutRewards;

    @Schema(description = "Grant ids whose grant_type is known-typed but the typed payload row is missing")
    private List<UUID> grantsWithoutTypedPayload;

    @Schema(description = "Grant ids attached to neither a group nor an option")
    private List<UUID> orphanGrants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FeatureGap")
    public static class FeatureGap {
        private UUID featureId;
        private String featureSlug;
        private String title;
        private String classSlug;
    }
}
