package com.dnd.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Класс FeatureRulesProperties описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.feature-rules")
public class FeatureRulesProperties {

    /** Master switch for the entire feature-rules runtime. */
    private boolean runtimeEnabled = false;

    /** Feature resources, charges and rest recovery (Stage 5). */
    private boolean resourcesEnabled = false;

    /** Feature action costs and feature use / action economy (Stage 6). */
    private boolean actionsEnabled = false;

    /** Active effects, conditions, modifiers and expiration (Stage 7). */
    private boolean effectsEnabled = false;

    /** Triggers, reactions and durable prompts (Stage 11). */
    private boolean triggersEnabled = false;

    /** Forms, transformations and companions (Stage 10). */
    private boolean formsEnabled = false;

    /** Spell / feature integration (Stage 9). */
    private boolean spellsEnabled = false;

    /** Item-granted feature rules: attunement, item-scoped resources and item actions. */
    private boolean itemsEnabled = false;

    /**
     * Выполняет операции "resources active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean resourcesActive() {
        return runtimeEnabled && resourcesEnabled;
    }

    /**
     * Выполняет операции "actions active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean actionsActive() {
        return runtimeEnabled && actionsEnabled;
    }

    /**
     * Выполняет операции "effects active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean effectsActive() {
        return runtimeEnabled && effectsEnabled;
    }

    /**
     * Выполняет операции "triggers active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean triggersActive() {
        return runtimeEnabled && triggersEnabled;
    }

    /**
     * Выполняет операции "forms active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean formsActive() {
        return runtimeEnabled && formsEnabled;
    }

    /**
     * Выполняет операции "spells active" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    public boolean spellsActive() {
        return runtimeEnabled && spellsEnabled;
    }

    /**
     * Проверяет, активна ли подсистема правил предметов.
     * @return true, если включён общий runtime и item-правила
     */
    public boolean itemsActive() {
        return runtimeEnabled && itemsEnabled;
    }
}
