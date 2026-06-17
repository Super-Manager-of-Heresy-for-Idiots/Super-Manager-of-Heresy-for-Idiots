package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Confirmation returned after creating a character on the new content model.
 * Intentionally minimal (it reports the new content IDs that were stored) and does
 * not reuse the legacy character response, whose class/skill mapping still reads the
 * legacy catalog. Full content-aware character detail is delivered in later phases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentCharacterCreationResult", description = "Result of creating a character on the new content model")
public class ContentCharacterCreationResponse {

    private UUID id;
    private String name;

    @Schema(description = "character_class id stored for this character")
    private UUID classId;

    private Integer totalLevel;

    @Schema(description = "Campaign id; null for standalone templates")
    private UUID campaignId;

    @Schema(description = "skill ids stored as class skill proficiencies")
    private List<UUID> skillProficiencyIds;

    @Schema(description = "spell ids stored as known spells")
    private List<UUID> knownSpellIds;
}
