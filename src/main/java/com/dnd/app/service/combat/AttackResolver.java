package com.dnd.app.service.combat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс AttackResolver описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class AttackResolver {

    /** Dice expression inside parentheses, e.g. the "1к4 + 3" of "5 ( 1к4 + 3 ) колющего урона". */
    private static final Pattern DICE_IN_PARENS = Pattern.compile(
            "\\(\\s*(\\d*\\s*[кКдДkKdD]\\s*\\d+(?:\\s*[+\\-]\\s*\\d+)?)\\s*\\)");
    /** Bare dice expression anywhere (fallback for descriptions that omit the parentheses). */
    private static final Pattern BARE_DICE = Pattern.compile(
            "(\\d+\\s*[кКдДkKdD]\\s*\\d+(?:\\s*[+\\-]\\s*\\d+)?)");
    /** Flat damage number right after the hit-clause colon, e.g. the "1" of "Попадание : 1 урона". */
    private static final Pattern FLAT_AFTER_COLON = Pattern.compile(":\\s*(\\d+)");

    private AttackResolver() {
    }

    /**
     * Перечисление Outcome описывает сервис боевой логики, который рассчитывает и применяет правила боя.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum Outcome {
        HIT, MISS, CRIT;

        /**
         * Выполняет операции "deals damage" в рамках бизнес-логики боя.
         * @return результат выполнения бизнес-операции
         */
        public boolean dealsDamage() {
            return this != MISS;
        }
    }

    /**
     * Перечисление SaveOutcome описывает сервис боевой логики, который рассчитывает и применяет правила боя.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum SaveOutcome {
        /** Target failed the save: full effect/damage. */
        FAIL,
        /** Target succeeded: typically half damage (or none). */
        SUCCESS
    }

    /**
     * Выполняет операции "resolve" в рамках бизнес-логики боя.
     * @param d20 входящее значение d20, используемое бизнес-сценарием
     * @param attackBonus входящее значение attack bonus, используемое бизнес-сценарием
     * @param targetAc входящее значение target ac, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Outcome resolve(int d20, int attackBonus, int targetAc) {
        if (d20 >= 20) {
            return Outcome.CRIT;
        }
        if (d20 <= 1) {
            return Outcome.MISS;
        }
        return (d20 + attackBonus >= targetAc) ? Outcome.HIT : Outcome.MISS;
    }

    /**
     * Выполняет операции "resolve save" в рамках бизнес-логики боя.
     * @param d20 входящее значение d20, используемое бизнес-сценарием
     * @param saveBonus входящее значение save bonus, используемое бизнес-сценарием
     * @param saveDc входящее значение save dc, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static SaveOutcome resolveSave(int d20, int saveBonus, int saveDc) {
        if (d20 >= 20) {
            return SaveOutcome.SUCCESS;
        }
        if (d20 <= 1) {
            return SaveOutcome.FAIL;
        }
        return (d20 + saveBonus >= saveDc) ? SaveOutcome.SUCCESS : SaveOutcome.FAIL;
    }

    /**
     * Выполняет операции "parse attack bonus" в рамках бизнес-логики боя.
     * @param bonus входящее значение bonus, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int parseAttackBonus(String bonus) {
        if (bonus == null || bonus.isBlank()) {
            return 0;
        }
        String cleaned = bonus.trim().replace("+", "").replaceAll("\\s", "");
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Выполняет операции "extract damage expression" в рамках бизнес-логики боя.
     * @param description входящее значение description, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static String extractDamageExpression(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String segment = description;
        for (String marker : new String[]{"Попадание", "Hit"}) {
            int idx = indexOfIgnoreCase(description, marker);
            if (idx >= 0) {
                segment = description.substring(idx);
                break;
            }
        }
        Matcher dice = DICE_IN_PARENS.matcher(segment);
        if (dice.find()) {
            return dice.group(1).trim();
        }
        Matcher bare = BARE_DICE.matcher(segment);
        if (bare.find()) {
            return bare.group(1).trim();
        }
        Matcher flat = FLAT_AFTER_COLON.matcher(segment);
        if (flat.find()) {
            return flat.group(1);
        }
        return null;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().indexOf(needle.toLowerCase());
    }
}
