package com.dnd.app.service.formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple map-backed {@link FormulaContext} for tests and admin preview. Keys:
 * scalars by name; class levels by {@code classKey}; ability mods by {@code abilityKey};
 * resource counts by {@code resourceKey}; target conditions by {@code conditionKey}.
 */
public class MapFormulaContext implements FormulaContext {

    private final Map<String, Double> scalars = new HashMap<>();
    private final Map<String, Double> classLevels = new HashMap<>();
    private final Map<String, Double> abilityMods = new HashMap<>();
    private final Map<String, Double> resourceCounts = new HashMap<>();
    private final Map<String, Boolean> targetConditions = new HashMap<>();

    public MapFormulaContext scalar(String name, double value) {
        scalars.put(name, value);
        return this;
    }

    public MapFormulaContext classLevel(String key, double value) {
        classLevels.put(key, value);
        return this;
    }

    public MapFormulaContext abilityMod(String key, double value) {
        abilityMods.put(key, value);
        return this;
    }

    public MapFormulaContext resourceCount(String key, double value) {
        resourceCounts.put(key, value);
        return this;
    }

    public MapFormulaContext targetCondition(String key, boolean value) {
        targetConditions.put(key, value);
        return this;
    }

    @Override
    public Double scalar(String name) {
        return scalars.get(name);
    }

    @Override
    public Double classLevel(String classKey) {
        return classLevels.get(classKey);
    }

    @Override
    public Double abilityMod(String abilityKey) {
        return abilityMods.get(abilityKey);
    }

    @Override
    public Double featureResourceCount(String resourceKey) {
        return resourceCounts.get(resourceKey);
    }

    @Override
    public Boolean targetCondition(String conditionKey) {
        return targetConditions.get(conditionKey);
    }
}
