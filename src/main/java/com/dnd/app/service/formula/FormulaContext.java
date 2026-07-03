package com.dnd.app.service.formula;

/**
 * Typed evaluation context for the formula DSL. Implementations resolve the allowlisted variables and
 * keyed accessors from a character/combat snapshot. Returning {@code null} means "not available", which
 * the evaluator turns into a controlled {@link FormulaException} rather than a silent 0.
 */
public interface FormulaContext {

    /** Bare scalars: character_level, proficiency_bonus, spell_slot_level, monster_cr, combat_round. */
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
