package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FeatureRuleProfile описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FeatureRuleProfile {

    /** Action / bonus action / reaction / free / special cost to use the feature. */
    ACTION_COST("action_cost", "Стоимость действия (action economy)"),
    /** Uses / charges pool for the feature. */
    RESOURCE("resource", "Ресурс, заряды, использования"),
    /** Recovery of a resource on short/long rest or another reset event. */
    REST_RESET("rest_reset", "Восстановление ресурса отдыхом/событием"),
    /** Static grant applied without a choice: proficiency, language, expertise, trait. */
    STATIC_GRANT("static_grant", "Статические grants (владения, языки, expertise)"),
    /** Player/GM choice with a set of options (skills, tools, forms, spells, etc.). */
    CHOICE("choice", "Выбор из вариантов при level-up/отдыхе"),
    /** Scaling / formula (resource max, duration, DC, dice, eligibility). */
    FORMULA("formula", "Формула/scaling"),
    /** Active effect / aura with duration, modifiers and stacking. */
    ACTIVE_EFFECT("active_effect", "Активный эффект/аура"),
    /** End condition for an effect (time, condition, rest, reuse). */
    END_CONDITION("end_condition", "Условие завершения эффекта"),
    /** Damage / temp HP output. */
    DAMAGE("damage", "Урон / временные HP"),
    /** Healing output. */
    HEALING("healing", "Лечение"),
    /** Saving throw / ability check / attack resolution with DC and outcomes. */
    SAVE_CHECK_ATTACK("save_check_attack", "Спасбросок / проверка / атака"),
    /** Spell grant / prepared / free cast / spellcasting override. */
    SPELL_GRANT("spell_grant", "Выдача/override заклинаний"),
    /** Allowed monster forms (Wild Shape-like) and transformation eligibility. */
    MONSTER_FORM("monster_form", "Формы/превращения (Wild Shape)"),
    /** Companion / construct / summoned creature bound to the character. */
    COMPANION("companion", "Спутники/конструкты"),
    /** Trigger / reaction hook fired by gameplay events. */
    TRIGGER_REACTION("trigger_reaction", "Триггеры/реакции"),
    /** Feature that cannot be safely automated and needs manual adjudication. */
    MANUAL_ADJUDICATION("manual_adjudication", "Ручное разрешение (нельзя автоматизировать)");

    private final String code;
    private final String description;

    FeatureRuleProfile(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Возвращает результат операции "get code" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public String getCode() {
        return code;
    }

    /**
     * Возвращает результат операции "get description" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public String getDescription() {
        return description;
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FeatureRuleProfile> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
