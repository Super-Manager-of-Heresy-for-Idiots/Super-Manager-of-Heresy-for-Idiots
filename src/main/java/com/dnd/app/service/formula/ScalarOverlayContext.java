package com.dnd.app.service.formula;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс ScalarOverlayContext описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class ScalarOverlayContext implements FormulaContext {

    private final FormulaContext base;
    private final Map<String, Double> overrides = new HashMap<>();

    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param base входящее значение base, используемое бизнес-сценарием
     */
    public ScalarOverlayContext(FormulaContext base) {
        this.base = base;
    }

    /**
     * Выполняет операции "scalar" в рамках бизнес-логики домена.
     * @param name входящее значение name, используемое бизнес-сценарием
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public ScalarOverlayContext scalar(String name, double value) {
        overrides.put(name, value);
        return this;
    }

    /**
     * Выполняет операции "scalar" в рамках бизнес-логики домена.
     * @param name входящее значение name, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double scalar(String name) {
        Double override = overrides.get(name);
        return override != null ? override : base.scalar(name);
    }

    /**
     * Выполняет операции "class level" в рамках бизнес-логики домена.
     * @param classKey входящее значение class key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double classLevel(String classKey) {
        return base.classLevel(classKey);
    }

    /**
     * Выполняет операции "ability mod" в рамках бизнес-логики домена.
     * @param abilityKey входящее значение ability key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double abilityMod(String abilityKey) {
        return base.abilityMod(abilityKey);
    }

    /**
     * Выполняет операции "feature resource count" в рамках бизнес-логики домена.
     * @param resourceKey входящее значение resource key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Double featureResourceCount(String resourceKey) {
        return base.featureResourceCount(resourceKey);
    }

    /**
     * Выполняет операции "target condition" в рамках бизнес-логики домена.
     * @param conditionKey входящее значение condition key, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public Boolean targetCondition(String conditionKey) {
        return base.targetCondition(conditionKey);
    }
}
