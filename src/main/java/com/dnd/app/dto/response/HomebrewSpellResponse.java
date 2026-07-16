package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO HomebrewSpellResponse — homebrew-заклинание (P2-1) для round-trip в редакторе.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewSpellResponse {

    private UUID id;
    private String name;
    private String nameEn;
    private Integer level;
    private String school;
    private String castingTimeRaw;
    // Структурные поля каста/дистанции/длительности/области (HB_UX Фазы 1/3) — для round-trip пикеров.
    private String castingActionSlug;
    private Integer castingTimeAmount;
    private String castingTimeUnit;
    private String reactionTriggerSlug;
    private Boolean ritual;
    private String rangeType;
    private Integer rangeDistance;
    private String rangeUnit;
    private String durationType;
    private Integer durationAmount;
    private String durationUnit;
    private String durationRaw;
    private String areaShape;
    private Integer areaSizeFt;
    private Boolean zonePersists;
    private String zoneTerrain;
    private String zoneObscurement;
    private Boolean concentration;
    private String description;
    private String higherLevels;
    private List<UUID> availableToClassIds;
    // Механика (P2-1 Phase B) — реконструируется из SPELL-owned feature_rules для round-trip.
    private String damageDice;
    private String damageType;
    private String saveAbility;
    private Boolean halfOnSave;
    private Boolean requiresAttackHit;
    private String healingFormula;
    // Состояния (HB_UX Фаза 4) — реконструируются из SPELL-owned active_effect-правила для round-trip.
    private List<String> conditionSlugs;
    private Integer conditionDurationRounds;
    private String source;
    private UUID homebrewPackageId;
    private String homebrewPackageTitle;
}
