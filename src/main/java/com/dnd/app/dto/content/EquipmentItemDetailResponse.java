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
 * Canonical read model for a single equipment item in the new content model (D&D 2024):
 * category, cost, weight, and—when applicable—weapon stats, armor stats and weapon properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EquipmentItemDetail", description = "Full read model for an equipment item")
public class EquipmentItemDetailResponse {

    private UUID id;
    private String slug;
    private String name;
    private String nameRu;
    private String nameEn;

    @Schema(example = "weapon", description = "weapon / armor / gear / tool / ...")
    private String kind;

    private ContentLabelDto category;
    private CostDto cost;
    private BigDecimal weightLb;
    private String propertiesText;
    private String url;

    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;

    @Schema(description = "Present only for weapons")
    private WeaponStatDto weaponStat;

    @Schema(description = "Present only for armor")
    private ArmorStatDto armorStat;

    private List<WeaponPropertyDto> weaponProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "EquipmentCost")
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
    @Schema(name = "DiceFormula")
    public static class DiceDto {
        private Integer diceCount;
        private Integer dieSize;
        private Integer bonus;
        private String rawText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WeaponStat")
    public static class WeaponStatDto {
        private DiceDto damageDice;
        private ContentLabelDto damageType;
        private Integer flatDamage;
        private ContentLabelDto mastery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ArmorStat")
    public static class ArmorStatDto {
        private Integer baseAc;
        private Boolean dexBonusAllowed;
        private Integer maxDexBonus;
        private Integer strengthRequired;
        private Boolean stealthDisadvantage;
        private String armorClassRaw;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WeaponItemProperty")
    public static class WeaponPropertyDto {
        private ContentLabelDto property;
        private Integer normalRangeFt;
        private Integer longRangeFt;
        private DiceDto versatileDice;
        private UUID ammunitionEquipmentItemId;
        private String rawText;
    }
}
