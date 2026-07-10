package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Класс FormulaContextPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaContextPayload {
    /** character_level, proficiency_bonus, spell_slot_level, monster_cr, combat_round. */
    private Map<String, Double> scalars;
    /** classKey -> level, for class_level("..."). */
    private Map<String, Double> classLevels;
    /** abilityKey -> modifier, for ability_mod("..."). */
    private Map<String, Double> abilityMods;
    /** resourceKey -> count, for feature_resource_count("..."). */
    private Map<String, Double> resourceCounts;
    /** conditionKey -> present, for target_condition("..."). */
    private Map<String, Boolean> targetConditions;
}
