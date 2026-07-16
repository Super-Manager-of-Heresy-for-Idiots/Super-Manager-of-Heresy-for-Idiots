package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO HomebrewItemResponse — единый homebrew-предмет (P1.5 / IT-2), пригодный для round-trip в редакторе.
 * Несёт как magic-поля (rarity/attunement), так и equipment-поля (вид/категория/стоимость/вес + weapon/armor секции)
 * для лосслесс-префилла формы правки.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewItemResponse {

    private UUID id;
    private String kind;
    private String name;
    private String nameEn;
    private String description;

    // --- MAGIC ---
    private String rarity;
    private Boolean attunementRequired;
    private String attunementRequirement;
    /** Структурное ограничение настройки (HB_UX Фаза 5): слаги классов/рас; enforced в /attune. */
    private List<String> attunementClassSlugs;
    private List<String> attunementRaceSlugs;

    // --- EQUIPMENT: общее ---
    private String equipmentKind;
    private String category;
    private BigDecimal costGold;
    private BigDecimal weightLb;

    // --- EQUIPMENT: weapon ---
    private Integer damageDiceCount;
    private Integer damageDieSize;
    private Integer damageBonus;
    private String damageType;
    private Integer flatDamage;

    // --- EQUIPMENT: armor ---
    private Integer baseAc;
    private Boolean dexBonusAllowed;
    private Integer maxDexBonus;
    private Integer strengthRequired;
    private Boolean stealthDisadvantage;

    // --- умение предмета (IT-4) — реконструируется из ITEM-owned feature_rules для round-trip ---
    private String abilityDamageDice;
    private String abilityDamageType;
    private String abilitySaveAbility;
    private Boolean abilityHalfOnSave;
    private String abilityHealingFormula;
    private Boolean abilityRequiresEquipped;
    private Boolean abilityRequiresAttunement;
    private Boolean abilityConsumeOnUse;

    private String source;
    private UUID homebrewPackageId;
    private String homebrewPackageTitle;
}
