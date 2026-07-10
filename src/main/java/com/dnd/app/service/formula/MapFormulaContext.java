package com.dnd.app.service.formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс MapFormulaContext описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class MapFormulaContext implements FormulaContext {

    private final Map<String, Double> scalars = new HashMap<>();
    private final Map<String, Double> classLevels = new HashMap<>();
    private final Map<String, Double> abilityMods = new HashMap<>();
    private final Map<String, Double> resourceCounts = new HashMap<>();
    private final Map<String, Boolean> targetConditions = new HashMap<>();

    /**
     * Выполняет операции "scalar" в рамках бизнес-логики домена.
     * @param name входящее значение name, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MapFormulaContext scalar(String name, double value) {
        scalars.put(name, value);
        return this;
    }

    /**
     * Выполняет операции "class level" в рамках бизнес-логики домена.
     * @param key входящее значение key, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MapFormulaContext classLevel(String key, double value) {
        classLevels.put(key, value);
        return this;
    }

    /**
     * Выполняет операции "ability mod" в рамках бизнес-логики домена.
     * @param key входящее значение key, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MapFormulaContext abilityMod(String key, double value) {
        abilityMods.put(key, value);
        return this;
    }

    /**
     * Выполняет операции "resource count" в рамках бизнес-логики домена.
     * @param key входящее значение key, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MapFormulaContext resourceCount(String key, double value) {
        resourceCounts.put(key, value);
        return this;
    }

    /**
     * Выполняет операции "target condition" в рамках бизнес-логики домена.
     * @param key входящее значение key, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MapFormulaContext targetCondition(String key, boolean value) {
        targetConditions.put(key, value);
        return this;
    }

    /**
     * Выполняет операции "scalar" в рамках бизнес-логики домена.
     * @param name входящее значение name, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double scalar(String name) {
        return scalars.get(name);
    }

    /**
     * Выполняет операции "class level" в рамках бизнес-логики домена.
     * @param classKey входящее значение class key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double classLevel(String classKey) {
        return classLevels.get(classKey);
    }

    /**
     * Выполняет операции "ability mod" в рамках бизнес-логики домена.
     * @param abilityKey входящее значение ability key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double abilityMod(String abilityKey) {
        return abilityMods.get(abilityKey);
    }

    /**
     * Выполняет операции "feature resource count" в рамках бизнес-логики домена.
     * @param resourceKey входящее значение resource key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double featureResourceCount(String resourceKey) {
        return resourceCounts.get(resourceKey);
    }

    /**
     * Выполняет операции "target condition" в рамках бизнес-логики домена.
     * @param conditionKey входящее значение condition key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Boolean targetCondition(String conditionKey) {
        return targetConditions.get(conditionKey);
    }
}
