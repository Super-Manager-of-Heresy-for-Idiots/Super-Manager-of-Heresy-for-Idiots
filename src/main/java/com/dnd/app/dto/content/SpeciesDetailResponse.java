package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс SpeciesDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SpeciesDetail", description = "Full read model for a species")
public class SpeciesDetailResponse {

    @Schema(description = "Species id")
    private UUID id;

    @Schema(description = "Slug, unique within scope", example = "elf")
    private String slug;

    @Schema(description = "Display name resolved for locale", example = "Elf")
    private String name;
    private String nameRu;
    private String nameEn;

    private String description;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    // Маркер происхождения (P0-4): GLOBAL — ванильный контент, HOMEBREW — homebrew.
    private String source;
    private String homebrewTitle;

    @Schema(description = "Creature type (humanoid, fey, ...)")
    private ContentLabelDto creatureType;

    @Schema(description = "Allowed sizes")
    private List<ContentLabelDto> sizeOptions;

    @Schema(description = "Movement speeds")
    private List<SpeedDto> speeds;

    @Schema(description = "Species traits with their mechanical effects")
    private List<TraitDto> traits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpeciesSpeed")
    public static class SpeedDto {
        @Schema(example = "walk")
        private String type;
        @Schema(example = "30")
        private Integer amountFt;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpeciesTrait")
    public static class TraitDto {
        private String slug;
        private String name;
        private String description;
        private List<EffectDto> effects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpeciesTraitEffect")
    public static class EffectDto {
        @Schema(example = "resistance")
        private String effectType;
        @Schema(description = "Damage type label, when the effect targets one")
        private ContentLabelDto damageType;
        @Schema(description = "Granted spell label, for innate-spell effects")
        private ContentLabelDto spell;
        private Integer rangeFt;
    }
}
