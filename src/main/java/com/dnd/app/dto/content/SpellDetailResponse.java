package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Canonical read model for a single spell in the new content model (D&D 2024).
 * Carries school, casting/range/duration metadata, components and the class/subclass
 * availability lists.
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

    private String description;
    private String higherLevels;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    private List<ComponentDto> components;

    @Schema(description = "Structured base damage entries (dice + type) detected for this spell")
    private List<DamageDto> damage;

    @Schema(description = "Classes that have this spell on their list")
    private List<ContentLabelDto> classes;

    @Schema(description = "Subclasses that grant this spell")
    private List<ContentLabelDto> subclasses;

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
}
