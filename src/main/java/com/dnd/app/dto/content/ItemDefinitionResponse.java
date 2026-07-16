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
 * Единый внешний read-DTO «Предмет» (IT-1). Скрывает физическое деление на три таблицы
 * (equipment_item / magic_item / item_templates): наружу игрок и GM видят одну сущность
 * «Предмет» с дискриминатором {@link #kind}. Общие поля (имя/описание/редкость/стоимость/
 * происхождение) — на верхнем уровне; kind-специфичные секции (weaponStat/armorStat/
 * weaponProperties для EQUIPMENT; attunement для MAGIC/TEMPLATE) заполняются по виду.
 *
 * <p>Собирается фабриками {@link #fromMagic}/{@link #fromEquipment} поверх уже существующих
 * detail-DTO (переиспользование мапперов), template маппится сервисом напрямую из сущности.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ItemDefinition", description = "Unified read model for an item definition (equipment / magic / legacy template)")
public class ItemDefinitionResponse {

    private UUID id;

    @Schema(example = "MAGIC", description = "EQUIPMENT | MAGIC | TEMPLATE (legacy)")
    private String kind;

    @Schema(description = "Stable slug; null for legacy templates")
    private String slug;

    private String name;
    private String nameRu;
    private String nameEn;
    private String description;

    @Schema(description = "Magic item type / template item-type / equipment category")
    private ContentLabelDto type;
    private ContentLabelDto rarity;
    private CostDto cost;
    private BigDecimal weightLb;
    private String url;

    // --- происхождение (P0-4) ---
    @Schema(description = "Homebrew package id; null for core content")
    private UUID packageId;
    private String source;
    private String homebrewTitle;

    // --- MAGIC / TEMPLATE ---
    private Boolean attunementRequired;
    private String attunementRequirement;

    /**
     * Даёт ли предмет исполняемые умения — есть ли у определения approved item-правило
     * (ITEM_ABIL Фаза 5, §5.7). Проставляется каталог-сервисом только при активной item-механике,
     * иначе false. Для бейджа «даёт умения» в диалоге выдачи предмета.
     */
    @Builder.Default
    private boolean grantsAbilities = false;

    // --- EQUIPMENT ---
    @Schema(example = "weapon", description = "weapon / armor / gear / tool — только для EQUIPMENT")
    private String equipmentKind;
    private EquipmentItemDetailResponse.WeaponStatDto weaponStat;
    private EquipmentItemDetailResponse.ArmorStatDto armorStat;
    private List<EquipmentItemDetailResponse.WeaponPropertyDto> weaponProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ItemDefinitionCost")
    public static class CostDto {
        private BigDecimal amount;
        private ContentLabelDto currency;
        private BigDecimal copperValue;
        private String rawText;
    }

    /**
     * Строит единый DTO из detail-DTO магического предмета (kind=MAGIC).
     * @param m detail-модель магического предмета (уже с source/packageId/homebrewTitle)
     * @return унифицированный «Предмет»
     */
    public static ItemDefinitionResponse fromMagic(MagicItemDetailResponse m) {
        CostDto cost = null;
        if (m.getCost() != null) {
            cost = CostDto.builder()
                    .amount(m.getCost().getAmount())
                    .currency(m.getCost().getCurrency())
                    .copperValue(m.getCost().getCopperValue())
                    .rawText(m.getCost().getRawText())
                    .build();
        }
        return ItemDefinitionResponse.builder()
                .id(m.getId())
                .kind("MAGIC")
                .slug(m.getSlug())
                .name(m.getName())
                .nameRu(m.getNameRu())
                .nameEn(m.getNameEn())
                .description(m.getDescription())
                .type(m.getType())
                .rarity(m.getRarity())
                .cost(cost)
                .url(m.getUrl())
                .packageId(m.getPackageId())
                .source(m.getSource())
                .homebrewTitle(m.getHomebrewTitle())
                .attunementRequired(m.getAttunementRequired())
                .attunementRequirement(m.getAttunementRequirement())
                .build();
    }

    /**
     * Строит единый DTO из detail-DTO снаряжения (kind=EQUIPMENT).
     * @param e detail-модель снаряжения (уже с source/packageId/homebrewTitle)
     * @return унифицированный «Предмет»
     */
    public static ItemDefinitionResponse fromEquipment(EquipmentItemDetailResponse e) {
        CostDto cost = null;
        if (e.getCost() != null) {
            cost = CostDto.builder()
                    .amount(e.getCost().getAmount())
                    .currency(e.getCost().getCurrency())
                    .copperValue(e.getCost().getCopperValue())
                    .rawText(e.getCost().getRawText())
                    .build();
        }
        return ItemDefinitionResponse.builder()
                .id(e.getId())
                .kind("EQUIPMENT")
                .slug(e.getSlug())
                .name(e.getName())
                .nameRu(e.getNameRu())
                .nameEn(e.getNameEn())
                .type(e.getCategory())
                .cost(cost)
                .weightLb(e.getWeightLb())
                .url(e.getUrl())
                .packageId(e.getPackageId())
                .source(e.getSource())
                .homebrewTitle(e.getHomebrewTitle())
                .equipmentKind(e.getKind())
                .weaponStat(e.getWeaponStat())
                .armorStat(e.getArmorStat())
                .weaponProperties(e.getWeaponProperties())
                .build();
    }
}
