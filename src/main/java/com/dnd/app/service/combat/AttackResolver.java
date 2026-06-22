package com.dnd.app.service.combat;

/**
 * Resolves a single attack roll against a target's armor class. A natural 20 always crits, a
 * natural 1 always misses; otherwise the attack hits when {@code d20 + attackBonus} reaches the
 * target's AC. Pure and side-effect free so the rules can be unit-tested in isolation — the dice
 * themselves are rolled by {@link DiceRoller}.
 */
public final class AttackResolver {

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
}
