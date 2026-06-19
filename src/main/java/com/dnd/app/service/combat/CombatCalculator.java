package com.dnd.app.service.combat;

import com.dnd.app.domain.BattleCombatant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pure combat math: ability modifiers, initiative, encounter danger/XP preview and the
 * deterministic ordering of the initiative tracker. Kept side-effect free and Spring-free
 * so the rules can be unit-tested in isolation.
 */
public final class CombatCalculator {

    private CombatCalculator() {
    }

    /** D&D ability modifier: floor((score - 10) / 2). */
    public static int abilityModifier(int score) {
        return (int) Math.floor((score - 10) / 2.0);
    }

    /**
     * A character's initiative: the d20 plus its Dexterity modifier and the net of any active
     * Dexterity STAT_MODIFIER buffs/debuffs (buffs add, debuffs subtract).
     */
    public static int characterInitiative(int d20, int dexScore, int dexBuffBonus) {
        return d20 + abilityModifier(dexScore) + dexBuffBonus;
    }

    /**
     * A monster's initiative: the d20 plus the authored initiative bonus when present,
     * otherwise the Dexterity modifier derived from its DEX score.
     */
    public static int monsterInitiative(int d20, Integer initiativeBonus, int dexScore) {
        int modifier = (initiativeBonus != null) ? initiativeBonus : abilityModifier(dexScore);
        return d20 + modifier;
    }

    /**
     * Average danger of the monster group = mean of the monsters' challenge ratings,
     * rounded to two decimals. Empty group → 0.
     */
    public static BigDecimal averageDanger(List<BigDecimal> crValues) {
        if (crValues == null || crValues.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = crValues.stream()
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(crValues.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Total combat XP. When the GM has overridden it, that value wins; otherwise it is the
     * sum of each monster's base XP (null base XP counts as 0).
     */
    public static int totalXp(List<Integer> xpBases, Integer override) {
        if (override != null) {
            return override;
        }
        if (xpBases == null) {
            return 0;
        }
        return xpBases.stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    /**
     * Sorts combatants by initiative (desc), breaking ties by Dexterity (desc) and finally by a
     * stable key (creation time then id) so the order is deterministic across recomputations.
     * Mutates each combatant's {@code turnOrder} to its 0-based position and returns the list.
     */
    public static List<BattleCombatant> orderTracker(List<BattleCombatant> combatants) {
        combatants.sort(Comparator
                .comparing(BattleCombatant::getInitiative, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BattleCombatant::getDexTiebreak, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(c -> c.getCreatedAt() == null ? Instant.EPOCH : c.getCreatedAt())
                .thenComparing(c -> c.getId() == null ? "" : c.getId().toString()));
        for (int i = 0; i < combatants.size(); i++) {
            combatants.get(i).setTurnOrder(i);
        }
        return combatants;
    }

    /**
     * After re-ordering the tracker, returns the index of the combatant that should keep the
     * turn. If the previously-active combatant is still present, the turn stays on it (its index
     * may have shifted because a faster combatant joined); otherwise the index is clamped into
     * range.
     */
    public static int resolveCurrentIndex(List<BattleCombatant> ordered, UUID activeCombatantId, int previousIndex) {
        if (activeCombatantId != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (activeCombatantId.equals(ordered.get(i).getId())) {
                    return i;
                }
            }
        }
        if (ordered.isEmpty()) {
            return 0;
        }
        return Math.min(Math.max(previousIndex, 0), ordered.size() - 1);
    }
}
