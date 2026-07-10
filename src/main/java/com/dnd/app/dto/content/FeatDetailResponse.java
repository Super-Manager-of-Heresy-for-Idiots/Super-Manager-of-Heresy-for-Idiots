package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс FeatDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeatDetail", description = "Full read model for a feat")
public class FeatDetailResponse {

    private UUID id;
    private String slug;
    private String name;
    private String nameRu;
    private String nameEn;
    private String description;
    private Boolean repeatable;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    @Schema(description = "Feat category (origin, general, fighting-style, ...)")
    private ContentLabelDto category;

    private List<PrerequisiteDto> prerequisites;
    private List<SectionDto> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FeatPrerequisite")
    public static class PrerequisiteDto {
        @Schema(example = "ability_score")
        private String type;
        private Integer levelRequired;
        @Schema(description = "Ability requirement, when the prerequisite targets one")
        private ContentLabelDto abilityScore;
        private Integer minimumScore;
        private String groupKey;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FeatSection")
    public static class SectionDto {
        private String title;
        private String body;
    }
}
