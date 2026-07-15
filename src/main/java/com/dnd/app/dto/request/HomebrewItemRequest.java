package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO HomebrewItemRequest — тело авторинга единого homebrew-предмета (P1.5 / IT-2). Снаружи предмет — одна
 * сущность; {@code kind} дискриминирует таблицу-назначение (MAGIC → magic_item; EQUIPMENT → equipment_item;
 * TEMPLATE запрещён для новых определений). Kind-специфичные поля: magic — rarity/attunement;
 * equipment — equipmentKind/category/cost/weight + weapon-секция (урон) / armor-секция (класс брони).
 */
@Data
public class HomebrewItemRequest {

    /** MAGIC | EQUIPMENT. */
    private String kind;

    @NotBlank(message = "Название предмета обязательно")
    @Size(max = 500)
    private String name;

    private String nameEn;

    private String description;

    // --- MAGIC ---
    /** Код редкости (magic). */
    private String rarity;

    private Boolean attunementRequired;

    private String attunementRequirement;

    // --- EQUIPMENT: общее ---
    /** Вид снаряжения: weapon | armor | gear | tool. */
    private String equipmentKind;

    /** Slug категории снаряжения (опционально; резолвится по ванили или пакету). */
    private String category;

    /** Стоимость в золотых монетах (опционально). */
    private BigDecimal costGold;

    /** Вес в фунтах (опционально). */
    private BigDecimal weightLb;

    // --- EQUIPMENT: weapon-секция ---
    private Integer damageDiceCount;

    private Integer damageDieSize;

    private Integer damageBonus;

    /** Slug типа урона (weapon). */
    private String damageType;

    private Integer flatDamage;

    // --- EQUIPMENT: armor-секция ---
    private Integer baseAc;

    private Boolean dexBonusAllowed;

    private Integer maxDexBonus;

    private Integer strengthRequired;

    private Boolean stealthDisadvantage;
}
