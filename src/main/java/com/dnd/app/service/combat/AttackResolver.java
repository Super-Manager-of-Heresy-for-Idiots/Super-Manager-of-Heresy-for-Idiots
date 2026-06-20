package com.dnd.app.service.combat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a single attack roll against a target's armor class. A natural 20 always crits, a
 * natural 1 always misses; otherwise the attack hits when {@code d20 + attackBonus} reaches the
 * target's AC. Pure and side-effect free so the rules can be unit-tested in isolation — the dice
 * themselves are rolled by {@link DiceRoller}.
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

    public enum Outcome {
        HIT, MISS, CRIT;

        public boolean dealsDamage() {
            return this != MISS;
        }
    }

    /** Outcome of a d20 attack vs the target's AC. */
    public static Outcome resolve(int d20, int attackBonus, int targetAc) {
        if (d20 >= 20) {
            return Outcome.CRIT;
        }
        if (d20 <= 1) {
            return Outcome.MISS;
        }
        return (d20 + attackBonus >= targetAc) ? Outcome.HIT : Outcome.MISS;
    }

    /** Parses a signed attack bonus such as "+5", "5" or "-1"; unparseable/blank → 0. */
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
     * Extracts a damage expression from a monster attack's free-text description, where weapon
     * damage is authored inline rather than in structured rows, e.g.
     * {@code "Попадание : 5 ( 1к4 + 3 ) колющего урона"} → {@code "1к4 + 3"}, or a flat
     * {@code "Попадание : 1 дробящего урона"} → {@code "1"}. Anchors on the hit clause to avoid
     * grabbing the to-hit bonus or reach. Returns {@code null} when no damage is present.
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
