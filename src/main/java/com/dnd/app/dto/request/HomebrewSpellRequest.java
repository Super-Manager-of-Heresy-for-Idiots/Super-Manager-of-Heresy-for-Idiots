package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO HomebrewSpellRequest — тело авторинга homebrew-заклинания (P2-1, Phase A: идентичность). Механика (урон/спасбросок/
 * эффекты) исполняется движком feature-rules (owner_type=SPELL) и добавляется отдельно; здесь — паспорт заклинания
 * (название, уровень, школа, время каста, дистанция, длительность, компоненты-описание, доступные классы).
 */
@Data
public class HomebrewSpellRequest {

    @NotBlank(message = "Название заклинания обязательно")
    @Size(max = 120)
    private String name;

    private String nameEn;

    @Min(0) @Max(9)
    private int level;

    /** Slug школы магии (резолвится). */
    @NotBlank(message = "Школа магии обязательна")
    private String school;

    // --- Время сотворения (HB_UX Фаза 1): структурный пикер вместо свободного текста. ---

    /** Слаг экономики действия: action | bonus-action | reaction | time. Пусто = «действие». */
    private String castingActionSlug;

    /** Для «долгого» каста (castingActionSlug=time): количество единиц времени. */
    @Min(1) @Max(1440)
    private Integer castingTimeAmount;

    /** Для «долгого» каста: единица времени (minute | hour). */
    private String castingTimeUnit;

    /** Слаг триггера реакции (trigger_event_type.code) для castingActionSlug=reaction; иначе null. */
    private String reactionTriggerSlug;

    private Boolean ritual;

    // --- Дистанция (HB_UX Фаза 1): структурный пикер. ---

    /** Тип дистанции: self | touch | distance | sight | unlimited. */
    private String rangeType;

    /** Для rangeType=distance: дистанция в футах. */
    @Min(0) @Max(100000)
    private Integer rangeDistance;

    /** Единица дистанции (ft); для distance. */
    private String rangeUnit;

    // --- Длительность (HB_UX Фаза 1): структурный пикер. ---

    /** Тип длительности: instantaneous | timed | until-dispelled | special. */
    private String durationType;

    /** Для timed: количество единиц (конвертируется в раунды детерминированно). */
    @Min(1) @Max(100000)
    private Integer durationAmount;

    /** Единица длительности: round | minute | hour | day. */
    private String durationUnit;

    private Boolean concentration;

    // --- Область действия (HB_UX Фаза 3): структурный пикер формы. ---

    /** Форма области: SPHERE | CUBE | CONE | CYLINDER | LINE; null = нет области. */
    private String areaShape;

    /** Размер формы в футах (радиус/ребро/длина). */
    @Min(0) @Max(1000)
    private Integer areaSizeFt;

    /** Зона остаётся на длительность (Web) vs мгновенная вспышка (Fireball). */
    private Boolean zonePersists;

    /** Труднопроходимая местность в зоне: DIFFICULT | null. */
    private String zoneTerrain;

    /** Затруднение видимости в зоне: LIGHT | HEAVY | null. */
    private String zoneObscurement;

    private String description;

    /** Текст «На больших уровнях» (апкаст). */
    private String higherLevels;

    /** Классы, которым доступно заклинание (spell_class). */
    private List<UUID> availableToClassIds;

    // --- механика (P2-1 Phase B): исполняется движком через SPELL-owned feature_rules ---

    /** Кости урона (напр. «8d6»); пусто — заклинание без урона. */
    private String damageDice;

    /** Slug типа урона (напр. «fire»). */
    private String damageType;

    /** Slug характеристики спасброска (напр. «dex»); задаёт спасбросок для урона/эффекта. */
    private String saveAbility;

    /** Половина урона при успешном спасброске. */
    private Boolean halfOnSave;

    /** Урон требует попадания атакой заклинанием. */
    private Boolean requiresAttackHit;

    /** Формула лечения (напр. «2d8 + wis_mod» или «2d8»); пусто — заклинание не лечит. */
    private String healingFormula;
}
