package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight reference label for any new-content entity (ability_score, skill,
 * feat, spell, subclass, …). Used for dropdowns and inline references in the
 * normalized content model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentLabel", description = "Reference label for a new-content entity")
public class ContentLabelDto {

    @Schema(description = "Server identifier", example = "0f9a1b2c-3d4e-5f60-7180-90a1b2c3d4e5")
    private UUID id;

    @Schema(description = "Stable slug, unique within scope", example = "arcana")
    private String slug;

    @Schema(description = "Display name resolved for the requested locale", example = "Arcana")
    private String name;

    @Schema(description = "Russian label", example = "Магия")
    private String nameRu;

    @Schema(description = "English label", example = "Arcana")
    private String nameEn;
}
