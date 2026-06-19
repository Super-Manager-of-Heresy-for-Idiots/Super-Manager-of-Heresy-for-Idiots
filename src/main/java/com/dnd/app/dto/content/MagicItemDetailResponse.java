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
 * Canonical read model for a single magic item in the new content model (D&D 2024):
 * type, rarity, attunement, cost and the equipment items it can be applied to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MagicItemDetail", description = "Full read model for a magic item")
public class MagicItemDetailResponse {

    private UUID id;
    private String slug;
    private String name;
    private String nameRu;
    private String nameEn;

    private ContentLabelDto type;
    private String typeRestrictionRaw;
    private ContentLabelDto rarity;
    private Boolean variableRarity;
    private Boolean attunementRequired;
    private String attunementRequirement;
    private CostDto cost;
    private String description;
    private Boolean embeddedTablesDetected;
    private String url;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    @Schema(description = "Base equipment items this magic item may be applied to")
    private List<AllowedEquipmentDto> allowedEquipment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MagicItemCost")
    public static class CostDto {
        private BigDecimal amount;
        private ContentLabelDto currency;
        private BigDecimal copperValue;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MagicItemAllowedEquipment")
    public static class AllowedEquipmentDto {
        private ContentLabelDto equipment;
        private String rawText;
    }
}
