package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Known numeric-modifier key suggestion for NUMERIC_MODIFIER grants. Free text
 * is still allowed by the authoring UI; these are only suggestions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReferenceModifierKey", description = "Numeric-modifier key suggestion")
public class ModifierKeyDto {

    @Schema(description = "Stable modifier key", example = "speed")
    private String key;

    @Schema(description = "Human-readable label", nullable = true)
    private String label;

    @Schema(description = "Default unit, when meaningful", nullable = true)
    private String defaultUnit;
}
