package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SpellcastingDto описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Spellcasting", description = "Class spellcasting profile (null => not a spellcaster)")
public class SpellcastingDto {

    @Schema(description = "Caster progression. Known: FULL | HALF | THIRD | PACT. Free text allowed for homebrew.",
            example = "FULL")
    private String casterProgression;

    @Schema(description = "Spellcasting ability (ability_score id)")
    private UUID spellcastingAbilityId;

    @Schema(description = "Resolved spellcasting ability label")
    private ContentLabelDto spellcastingAbility;

    @Schema(description = "Preparation model. Known: PREPARED | KNOWN.", example = "KNOWN")
    private String preparation;

    @Schema(description = "Whether the class can ritual-cast", example = "false")
    private Boolean ritualCasting;

    @Schema(description = "True if the class learns cantrips", example = "true")
    private Boolean hasCantrips;

    @Schema(description = "Convenience flag: half-caster progression", example = "false")
    private Boolean halfCaster;

    @Schema(description = "Free-text spellcasting focus", example = "An arcane focus or a component pouch")
    private String spellcastingFocusText;

    @Schema(description = "Free-text notes")
    private String notes;
}
