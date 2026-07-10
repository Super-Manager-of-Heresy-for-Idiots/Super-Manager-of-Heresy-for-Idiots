package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс FeatureGrantPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeatureGrantPayload", description = "Grants a class feature (payload for grantType=FEATURE)")
public class FeatureGrantPayload implements GrantPayload {

    @Schema(description = "Reference to an existing class_feature id (preferred on update)")
    private UUID featureId;

    @Schema(description = "Reference to a feature created in the same write request (by client key)")
    private String featureKey;

    @Schema(description = "Inline one-off feature (server materializes a class_feature)")
    private InlineFeature inline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "InlineFeature", description = "Short inline feature definition")
    public static class InlineFeature {
        @Schema(example = "Storm Sense")
        private String title;
        private String description;
    }
}
