package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Feat reference option for authoring dropdowns (class builder FEAT grants).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReferenceFeatOption", description = "Feat reference option for authoring dropdowns")
public class FeatOptionDto {

    @Schema(description = "Server identifier")
    private UUID id;

    @Schema(description = "Stable slug, unique within scope")
    private String slug;

    @Schema(description = "Display name resolved for the requested locale")
    private String name;

    @Schema(description = "Prerequisite text, when modeled", nullable = true)
    private String prerequisiteText;
}
