package com.dnd.app.service.formula;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link FormulaContext} decorator that overrides/adds a few bare scalars on top of a base context.
 * Used for cast-time values the character snapshot cannot know in advance — chiefly
 * {@code spell_slot_level} (the slot level a spell is actually cast with).
 */
public class ScalarOverlayContext implements FormulaContext {

    private final FormulaContext base;
    private final Map<String, Double> overrides = new HashMap<>();

    public ScalarOverlayContext(FormulaContext base) {
        this.base = base;
    }

    public ScalarOverlayContext scalar(String name, double value) {
        overrides.put(name, value);
        return this;
    }

    @Override
    public Double scalar(String name) {
        Double override = overrides.get(name);
        return override != null ? override : base.scalar(name);
    }

    @Override
    public Double classLevel(String classKey) {
        return base.classLevel(classKey);
    }

    @Override
    public Double abilityMod(String abilityKey) {
        return base.abilityMod(abilityKey);
    }

    @Override
    public Double featureResourceCount(String resourceKey) {
        return base.featureResourceCount(resourceKey);
    }

    @Override
    public Boolean targetCondition(String conditionKey) {
        return base.targetCondition(conditionKey);
    }
}
