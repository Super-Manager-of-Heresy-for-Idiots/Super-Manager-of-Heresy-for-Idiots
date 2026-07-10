package com.dnd.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Класс ClassFeatureResolutionRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Schema(name = "ClassFeatureResolutionRequest")
public class ClassFeatureResolutionRequest {

    @Schema(description = "PASSIVE, ACTION, BONUS_ACTION, or REACTION")
    private String activationType;

    private Boolean attackRoll;

    @Schema(description = "Saving-throw ability code (STRENGTH..CHARISMA); null/blank clears it")
    private String saveAbility;

    private String damageDice;
    private String damageType;
    private String healingDice;
    private Integer healingFlat;

    @Schema(description = "Keep the record flagged for review; typically false once corrected")
    private Boolean warning;
}
