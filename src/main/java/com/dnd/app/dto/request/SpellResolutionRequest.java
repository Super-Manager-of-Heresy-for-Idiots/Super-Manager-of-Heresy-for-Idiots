package com.dnd.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Класс SpellResolutionRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Schema(name = "SpellResolutionRequest")
public class SpellResolutionRequest {

    @Schema(description = "Saving-throw ability code (STRENGTH..CHARISMA); null/blank clears it", example = "DEXTERITY")
    private String saveAbility;

    @Schema(description = "Whether the spell resolves with an attack roll")
    private Boolean attackRoll;

    @Schema(description = "Keep the record flagged for review; typically false once corrected")
    private Boolean warning;
}
