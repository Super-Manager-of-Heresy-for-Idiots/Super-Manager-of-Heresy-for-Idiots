package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SpellWarningResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SpellWarning", description = "A spell flagged for manual resolution review")
public class SpellWarningResponse {

    private UUID id;
    private String slug;
    private String name;
    private Integer level;
    private String schoolName;

    @Schema(description = "Parsed saving-throw ability (STRENGTH..CHARISMA) or null")
    private String saveAbility;
    private Boolean attackRoll;

    @Schema(description = "Whether the record still needs manual review")
    private Boolean warning;
    @Schema(description = "Stable reason code the UI localises, e.g. SAVE_UNRESOLVED")
    private String warningReason;

    private String description;
}
