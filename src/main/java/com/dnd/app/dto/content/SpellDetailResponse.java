package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс SpellDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SpellDetail", description = "Full read model for a spell")
public class SpellDetailResponse {

    private UUID id;
    private String slug;
    private String name;
    private String nameRu;
    private String nameEn;
    private Integer level;

    @Schema(description = "School of magic (evocation, illusion, ...)")
    private ContentLabelDto school;

    private String castingTimeRaw;
    private String castingActionSlug;
    private Boolean ritual;

    private String rangeType;
    private Integer rangeDistance;
    private String rangeUnit;

    private String durationRaw;
    private String durationType;
    private Integer durationAmount;
    private String durationUnit;
    private Boolean concentration;

    @Schema(description = "Ability the target saves with (STRENGTH..CHARISMA); null if the spell forces no save. "
            + "The DC is not stored — it is computed per caster (8 + proficiency + spellcasting modifier).")
    private String saveAbility;

    @Schema(description = "True when the spell resolves with an attack roll rather than a saving throw")
    private Boolean attackRoll;

    @Schema(description = "Ability the target resists with via an ability CHECK (проверка), e.g. INTELLIGENCE. "
            + "Unlike a save, a check benefits from the creature's proficiency/skill bonuses. Null if none.")
    private String checkAbility;

    @Schema(description = "Skill named alongside the ability check, raw RU text (may be a choice like "
            + "'Восприятие или Выживание'). Null when the check names no skill.")
    private String checkSkill;

    private String description;
    private String higherLevels;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    @Schema(description = "Origin marker: GLOBAL for core/vanilla content, HOMEBREW for homebrew content (P0-4)")
    private String source;

    @Schema(description = "Homebrew package title; null for core content")
    private String homebrewTitle;

    @Schema(description = "Spell art: media proxy URL (/api/media/{id}/content) or null")
    private String artUrl;

    @Schema(description = "True when the spell's auto-derived resolution was flagged for manual admin review")
    private Boolean warning;

    @Schema(description = "Machine code for why the spell was flagged (e.g. SAVE_UNRESOLVED); null when not flagged")
    private String warningReason;

    private List<ComponentDto> components;

    @Schema(description = "Structured base damage entries (dice + type) detected for this spell")
    private List<DamageDto> damage;

    @Schema(description = "Structured healing entries (dice and/or flat HP restored) detected for this spell")
    private List<HealingDto> healing;

    @Schema(description = "Classes that have this spell on their list")
    private List<ContentLabelDto> classes;

    @Schema(description = "Subclasses that grant this spell")
    private List<ContentLabelDto> subclasses;

    @Schema(description = "Bestiary statblocks this summon/conjuration spell summons or whose stats it uses. "
            + "Empty for non-summon spells. Each entry carries the monster id so the client can link "
            + "straight to the bestiary detail view.")
    private List<SummonedMonsterDto> summonedMonsters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SummonedMonster", description = "A bestiary statblock referenced by a summon spell")
    public static class SummonedMonsterDto {
        @Schema(description = "Monster id — link target for /bestiary/monsters/{id}")
        private UUID id;
        private String slug;
        private String name;
        private String nameRu;
        private String nameEn;
        @Schema(example = "1/4", description = "Challenge rating label")
        private String crRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpellComponent")
    public static class ComponentDto {
        @Schema(example = "material")
        private String component;
        private String materialText;
        private Boolean consumed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpellDamage", description = "One structured base-damage entry of a spell")
    public static class DamageDto {
        @Schema(example = "1d6", description = "Dice formula, canonicalised to NdM")
        private String dice;
        @Schema(description = "Damage type reference (null when unresolved)")
        private ContentLabelDto damageType;
        @Schema(description = "Original raw damage text from the source")
        private String raw;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpellHealing", description = "One structured healing entry of a spell")
    public static class HealingDto {
        @Schema(example = "2d8", description = "Dice formula, canonicalised to NdM; null for a flat-only heal")
        private String dice;
        @Schema(example = "70", description = "Flat hit points restored; null when the heal is dice-based")
        private Integer flat;
        @Schema(description = "Original raw healing text from the source")
        private String raw;
    }
}
