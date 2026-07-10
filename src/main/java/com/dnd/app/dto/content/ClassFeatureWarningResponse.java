package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ClassFeatureWarningResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassFeatureWarning", description = "A class feature flagged for mechanics review")
public class ClassFeatureWarningResponse {

    private UUID id;
    private String slug;
    private String title;
    private String className;
    private String subclassName;
    private Integer level;
    private String activationType;
    private Boolean attackRoll;
    private String saveAbility;
    private String damageDice;
    private String damageType;
    private String healingDice;
    private Integer healingFlat;
    private Boolean warning;
    private String warningReason;
    private String description;
}
