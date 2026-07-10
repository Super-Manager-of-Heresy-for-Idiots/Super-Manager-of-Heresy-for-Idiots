package com.dnd.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Класс SpellEditRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Schema(name = "SpellEditRequest")
public class SpellEditRequest {

    @Schema(description = "Saving-throw ability code (STRENGTH..CHARISMA); null/blank clears it", example = "DEXTERITY")
    private String saveAbility;

    @Schema(description = "Whether the spell resolves with an attack roll")
    private Boolean attackRoll;

    @Schema(description = "Ability-check ability code (STRENGTH..CHARISMA); null/blank clears it", example = "INTELLIGENCE")
    private String checkAbility;

    @Schema(description = "Skill named alongside the ability check (raw text, may be a choice); null/blank clears it",
            example = "Расследование")
    private String checkSkill;

    @Schema(description = "Keep the record flagged for review; typically false once corrected")
    private Boolean warning;

    @Schema(description = "Full replacement set of base damage entries; null leaves damage untouched, empty clears it")
    private List<DamageRow> damages;

    @Schema(description = "Full replacement set of healing entries; null leaves healing untouched, empty clears it")
    private List<HealingRow> healings;

    @Data
    @Schema(name = "SpellDamageEditRow")
    public static class DamageRow {
        @Schema(description = "Dice formula (e.g. 2d6); canonicalised on save", example = "2d6")
        private String dice;
        @Schema(description = "Damage-type slug from /reference/damage-types; null for typeless", example = "fire")
        private String damageTypeSlug;
        @Schema(description = "Optional raw source text")
        private String raw;
    }

    @Data
    @Schema(name = "SpellHealingEditRow")
    public static class HealingRow {
        @Schema(description = "Dice formula (e.g. 2d8); canonicalised on save", example = "2d8")
        private String dice;
        @Schema(description = "Flat hit points restored", example = "70")
        private Integer flat;
        @Schema(description = "Optional raw source text")
        private String raw;
    }
}
