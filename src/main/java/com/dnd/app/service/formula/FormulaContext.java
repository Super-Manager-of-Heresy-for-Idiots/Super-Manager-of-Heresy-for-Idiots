package com.dnd.app.service.formula;

/**
 * Контракт FormulaContext описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FormulaContext {

    /**
     * Bare scalars: character_level, proficiency_bonus, spell_slot_level, spellcasting_ability_mod,
     * monster_cr, combat_round.
     */
    Double scalar(String name);

    /** class_level("Druid"). */
    Double classLevel(String classKey);

    /** ability_mod("CHA"). */
    Double abilityMod(String abilityKey);

    /** feature_resource_count("rage"). */
    Double featureResourceCount(String resourceKey);

    /** target_condition("prone") predicate; null = unknown. */
    default Boolean targetCondition(String conditionKey) {
        return null;
    }
}
