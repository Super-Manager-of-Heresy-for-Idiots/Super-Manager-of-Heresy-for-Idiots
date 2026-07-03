package com.dnd.app.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort parser for imported class-feature prose. The result is intentionally
 * conservative: ambiguous rows are flagged so admins can review them before the
 * runtime relies on the parsed mechanics.
 */
public final class ClassFeatureMechanicParser {

    public static final String WARN_SAVE_UNRESOLVED = "SAVE_UNRESOLVED";
    public static final String WARN_DAMAGE_UNRESOLVED = "DAMAGE_UNRESOLVED";
    public static final String WARN_HEALING_UNRESOLVED = "HEALING_UNRESOLVED";
    public static final String WARN_MULTIPLE_DICE = "MULTIPLE_DICE";

    private static final Pattern DICE = Pattern.compile(
            "(\\d+)\\s*[\\u043a\\u041akKdD\\u0434\\u0414]\\s*(\\d+)(?:\\s*([+\\-])\\s*(\\d+))?");
    private static final Pattern FLAT_HEAL = Pattern.compile(
            "(\\d+)\\s*(?:\\u0445\\u0438\\u0442|hit)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SAVE = Pattern.compile(
            "\\u0441\\u043f\\u0430\\u0441\\u0431\\u0440\\u043e\\u0441\\p{L}*\\s+"
                    + "(\\u0421\\u0438\\u043b|\\u041b\\u043e\\u0432\\u043a\\u043e\\u0441\\u0442|"
                    + "\\u0422\\u0435\\u043b\\u043e\\u0441\\u043b\\u043e\\u0436\\u0435\\u043d\\u0438|"
                    + "\\u0418\\u043d\\u0442\\u0435\\u043b\\u043b\\u0435\\u043a\\u0442|"
                    + "\\u041c\\u0443\\u0434\\u0440\\u043e\\u0441\\u0442|\\u0425\\u0430\\u0440\\u0438\\u0437\\u043c)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private ClassFeatureMechanicParser() {
    }

    public static Result parse(String title, String description) {
        String text = join(title, description);
        if (text.isBlank()) {
            return Result.empty();
        }

        String lower = text.toLowerCase(Locale.ROOT);
        List<String> dice = diceExpressions(text);
        boolean damageMentioned = mentionsDamage(lower);
        boolean healingMentioned = mentionsHealing(lower);

        String damageDice = damageMentioned && !dice.isEmpty() ? dice.get(0) : null;
        String healingDice = healingMentioned && !dice.isEmpty() ? dice.get(0) : null;
        Integer healingFlat = healingMentioned && dice.isEmpty() ? flatHealing(text) : null;
        String warningReason = null;

        String saveAbility = detectSaveAbility(text);
        if (saveAbility == null && mentionsSave(lower)) {
            warningReason = WARN_SAVE_UNRESOLVED;
        } else if (damageMentioned && damageDice == null) {
            warningReason = WARN_DAMAGE_UNRESOLVED;
        } else if (healingMentioned && healingDice == null && healingFlat == null) {
            warningReason = WARN_HEALING_UNRESOLVED;
        } else if (dice.size() > 1) {
            warningReason = WARN_MULTIPLE_DICE;
        }

        return new Result(
                detectActivation(lower),
                mentionsAttackRoll(lower),
                saveAbility,
                damageDice,
                detectDamageType(lower),
                healingDice,
                healingFlat,
                warningReason != null,
                warningReason);
    }

    private static String join(String title, String description) {
        return ((title != null ? title : "") + " " + (description != null ? description : "")).trim();
    }

    private static List<String> diceExpressions(String text) {
        Matcher matcher = DICE.matcher(text);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String dice = matcher.group(1) + "d" + matcher.group(2);
            if (matcher.group(3) != null && matcher.group(4) != null) {
                dice += " " + matcher.group(3) + " " + matcher.group(4);
            }
            result.add(dice);
        }
        return result;
    }

    private static Integer flatHealing(String text) {
        Matcher matcher = FLAT_HEAL.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private static String detectSaveAbility(String text) {
        Matcher matcher = SAVE.matcher(text);
        return matcher.find() ? abilityCode(matcher.group(1)) : null;
    }

    private static String abilityCode(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.startsWith("\u0441\u0438\u043b")) return "STRENGTH";
        if (value.startsWith("\u043b\u043e\u0432\u043a\u043e\u0441\u0442")) return "DEXTERITY";
        if (value.startsWith("\u0442\u0435\u043b\u043e\u0441\u043b\u043e\u0436\u0435\u043d\u0438")) return "CONSTITUTION";
        if (value.startsWith("\u0438\u043d\u0442\u0435\u043b\u043b\u0435\u043a\u0442")) return "INTELLIGENCE";
        if (value.startsWith("\u043c\u0443\u0434\u0440\u043e\u0441\u0442")) return "WISDOM";
        if (value.startsWith("\u0445\u0430\u0440\u0438\u0437\u043c")) return "CHARISMA";
        return null;
    }

    private static String detectActivation(String lower) {
        if (containsAny(lower, "\u0431\u043e\u043d\u0443\u0441\u043d\u044b\u043c \u0434\u0435\u0439\u0441\u0442\u0432", "bonus action")) {
            return "BONUS_ACTION";
        }
        if (containsAny(lower, "\u0440\u0435\u0430\u043a\u0446", "reaction")) {
            return "REACTION";
        }
        if (containsAny(lower, "\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435\u043c", "\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f", " action")) {
            return "ACTION";
        }
        return "PASSIVE";
    }

    private static boolean mentionsAttackRoll(String lower) {
        return containsAny(lower,
                "\u0431\u0440\u043e\u0441\u043e\u043a \u0430\u0442\u0430\u043a",
                "\u0431\u0440\u043e\u0441\u043a\u0430 \u0430\u0442\u0430\u043a",
                "attack roll");
    }

    private static boolean mentionsSave(String lower) {
        return containsAny(lower, "\u0441\u043f\u0430\u0441\u0431\u0440\u043e\u0441", "saving throw");
    }

    private static boolean mentionsDamage(String lower) {
        return containsAny(lower,
                "\u0443\u0440\u043e\u043d",
                "damage",
                "\u043f\u043e\u043f\u0430\u0434\u0430\u043d\u0438\u0435");
    }

    private static boolean mentionsHealing(String lower) {
        return containsAny(lower,
                "\u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432",
                "\u043b\u0435\u0447\u0435\u043d",
                "\u0438\u0441\u0446\u0435\u043b",
                "restore",
                "regain",
                "heal");
    }

    private static String detectDamageType(String lower) {
        if (containsAny(lower, "\u0440\u0443\u0431\u044f\u0449", "slashing")) return "SLASHING";
        if (containsAny(lower, "\u043a\u043e\u043b\u044e\u0449", "piercing")) return "PIERCING";
        if (containsAny(lower, "\u0434\u0440\u043e\u0431\u044f\u0449", "bludgeoning")) return "BLUDGEONING";
        if (containsAny(lower, "\u043e\u0433\u043d", "fire")) return "FIRE";
        if (containsAny(lower, "\u0445\u043e\u043b\u043e\u0434", "cold")) return "COLD";
        if (containsAny(lower, "\u044d\u043b\u0435\u043a\u0442\u0440", "\u043c\u043e\u043b\u043d\u0438", "lightning")) return "LIGHTNING";
        if (containsAny(lower, "\u044f\u0434", "poison")) return "POISON";
        if (containsAny(lower, "\u043d\u0435\u043a\u0440\u043e\u0442", "necrotic")) return "NECROTIC";
        if (containsAny(lower, "\u0438\u0437\u043b\u0443\u0447", "radiant")) return "RADIANT";
        if (containsAny(lower, "\u043f\u0441\u0438\u0445", "psychic")) return "PSYCHIC";
        if (containsAny(lower, "\u0441\u0438\u043b\u043e\u0432", "force")) return "FORCE";
        if (containsAny(lower, "\u0437\u0432\u0443\u043a", "\u0433\u0440\u043e\u043c", "thunder")) return "THUNDER";
        if (containsAny(lower, "\u043a\u0438\u0441\u043b\u043e\u0442", "acid")) return "ACID";
        return null;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record Result(
            String activationType,
            boolean attackRoll,
            String saveAbility,
            String damageDice,
            String damageType,
            String healingDice,
            Integer healingFlat,
            boolean warning,
            String warningReason) {

        static Result empty() {
            return new Result("PASSIVE", false, null, null, null, null, null, false, null);
        }
    }
}
