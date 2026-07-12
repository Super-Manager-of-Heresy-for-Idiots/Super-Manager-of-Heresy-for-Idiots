package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ContentClassDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentClassDetail", description = "Full read model for a class")
public class ContentClassDetailResponse {

    @Schema(description = "Class id")
    private UUID id;

    @Schema(description = "Slug, unique within scope", example = "stormbinder")
    private String slug;

    @Schema(description = "Display name resolved for locale", example = "Stormbinder")
    private String name;
    private String nameRu;
    private String nameEn;

    @Schema(example = "Storm caster")
    private String subtitle;

    private String description;

    @Schema(description = "Homebrew package id; null for admin/core content")
    private UUID packageId;

    // Маркер происхождения (P0-4): GLOBAL — ванильный контент, HOMEBREW — homebrew.
    private String source;
    private String homebrewTitle;

    // core mechanics
    @Schema(description = "Hit die (6/8/10/12)", example = "8")
    private Integer hitDie;

    @Schema(description = "Primary abilities")
    private List<ContentLabelDto> primaryAbilities;

    @Schema(description = "Saving throw proficiencies")
    private List<ContentLabelDto> savingThrows;

    @Schema(description = "How many skills the player chooses at creation", example = "2")
    private Integer skillChoiceCount;

    @Schema(description = "True => choose from all skills (skillOptions ignored)", example = "false")
    private Boolean skillChoiceAny;

    @Schema(description = "Skill pool when skillChoiceAny=false")
    private List<ContentLabelDto> skillOptions;

    @Schema(example = "Light armor")
    private String armorProficiencyText;
    @Schema(example = "Simple weapons")
    private String weaponProficiencyText;
    private String toolProficiencyText;

    @Schema(description = "Spellcasting profile; null => not a spellcaster")
    private SpellcastingDto spellcasting;

    @Schema(description = "All class + subclass features")
    private List<ClassFeatureSummaryDto> features;

    @Schema(description = "Reward groups across all class levels")
    private List<RewardGroupDto> rewardGroups;
}
