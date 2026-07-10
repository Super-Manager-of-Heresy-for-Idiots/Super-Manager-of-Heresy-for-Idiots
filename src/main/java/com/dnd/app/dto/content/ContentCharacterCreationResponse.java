package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ContentCharacterCreationResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
