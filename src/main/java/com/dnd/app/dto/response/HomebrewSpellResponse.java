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
    private Boolean ritual;
    private String rangeText;
    private String durationText;
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
    private String source;
    private UUID homebrewPackageId;
    private String homebrewPackageTitle;
}
