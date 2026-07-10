package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Класс BackgroundDetailResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BackgroundDetail", description = "Full read model for a background")
public class BackgroundDetailResponse {

    private UUID id;
    private String slug;
    private String name;
    private String nameRu;
    private String nameEn;
    private String description;
    private String url;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    @Schema(description = "Feat granted by this background (2024 origin feat)")
    private ContentLabelDto grantedFeat;

    @Schema(description = "Ability scores eligible for the background's ability-score increase")
    private List<ContentLabelDto> abilityOptions;

    @Schema(description = "Skill proficiencies granted by this background")
    private List<ContentLabelDto> skillProficiencies;

    private List<FeatOptionDto> featOptions;
    private List<ToolProficiencyDto> toolProficiencies;
    private List<LanguageProficiencyDto> languageProficiencies;
    private List<EquipmentGroupDto> equipmentChoiceGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundFeatOption")
    public static class FeatOptionDto {
        private ContentLabelDto feat;
        private ContentLabelDto featCategory;
        private Integer chooseCount;
        private String selectedOptionRaw;
        private ContentLabelDto recommendedFeat;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundToolProficiency")
    public static class ToolProficiencyDto {
        private UUID equipmentItemId;
        private Integer chooseCount;
        private String choiceGroupSlug;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundLanguageProficiency")
    public static class LanguageProficiencyDto {
        private String languageSlug;
        private Integer chooseCount;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundEquipmentGroup")
    public static class EquipmentGroupDto {
        private String groupSlug;
        private Integer chooseCount;
        private String rawText;
        private List<EquipmentOptionDto> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundEquipmentOption")
    public static class EquipmentOptionDto {
        private String optionCode;
        private Integer sortOrder;
        private String rawText;
        private List<EquipmentEntryDto> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BackgroundEquipmentEntry")
    public static class EquipmentEntryDto {
        private String entryType;
        private UUID equipmentItemId;
        private UUID moneyValueId;
        private BigDecimal quantity;
        private String quantityUnitRaw;
        private String variantNote;
        private String choiceRef;
        private String rawText;
    }
}
