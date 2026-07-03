package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Canonical set of "rule profiles" for class-feature mechanics (see
 * {@code docs/CLASS_FEATURE_MECHANICS_AUDIT_2026-07-03.md} and
 * {@code anal-integration/00_STAGE_SCOPE_AND_EXECUTION_CONTRACT.md}).
 *
 * <p>A single class feature ("умение") is a composition of one or more independent rule rows.
 * Each rule row carries a profile, which decides what specialized table/runtime handles it. The
 * {@link #getCode() code} of each profile is the stable identifier stored in the
 * {@code feature_rule.rule_type} column and seeded into the {@code feature_rule_type} reference
 * table in Stage 1.</p>
 *
 * <p>This enum only fixes the vocabulary; no runtime executes these profiles yet (Stage 0 does not
 * change application behavior).</p>
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

    /** Stable snake_case identifier used in persistence and API payloads. */
    public String getCode() {
        return code;
    }

    /** Human-readable description for admin UIs / documentation. */
    public String getDescription() {
        return description;
    }

    /** Resolve a profile from its persisted {@link #getCode() code}. */
    public static Optional<FeatureRuleProfile> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(p -> p.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
