package com.dnd.app.service.combat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed dice expression — {@code count}d{@code sides} ± {@code modifier}. Supports the forms
 * authored on character attacks and monster features: "2d6+3", "1d8", "d20", "1к8" (Cyrillic die
 * marker) and flat numbers like "5". Pure and side-effect free so it can be unit-tested; the
 * actual rolling lives in {@link DiceRoller}.
 */
public record DiceExpression(int count, int sides, int modifier) {

    private static final Pattern DICE = Pattern.compile(
            "\\s*(\\d*)\\s*[dDкКkK]\\s*(\\d+)\\s*([+\\-]\\s*\\d+)?\\s*");
    private static final Pattern FLAT = Pattern.compile("\\s*([+\\-]?\\d+)\\s*");

    /** Parses an expression, returning {@code null} when nothing usable is present. */
    public static DiceExpression parse(String expr) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        Matcher m = DICE.matcher(expr);
        if (m.matches()) {
            int count = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
            int sides = Integer.parseInt(m.group(2));
            int mod = (m.group(3) == null) ? 0 : Integer.parseInt(m.group(3).replaceAll("\\s", ""));
            return new DiceExpression(count, sides, mod);
        }
        Matcher f = FLAT.matcher(expr);
        if (f.matches()) {
            return new DiceExpression(0, 0, Integer.parseInt(f.group(1)));
        }
        return null;
    }

    /** Smallest possible total (every die shows 1). */
    public int min() {
        return count + modifier;
    }

    /** Largest possible total (every die shows its max face). */
    public int max() {
        return count * sides + modifier;
    }
}
