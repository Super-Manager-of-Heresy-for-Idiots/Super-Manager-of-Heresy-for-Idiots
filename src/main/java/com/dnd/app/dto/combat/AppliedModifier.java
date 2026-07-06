package com.dnd.app.dto.combat;

/**
 * One modifier contributing to a {@link ModifierTarget}, tagged with where it came from and a
 * {@code stackKey}. Modifiers sharing a stackKey do not stack — the aggregator keeps only the
 * largest — which is how "+2 from an item and the same +2 re-expressed as a feature" collapses to
 * +2 instead of +4 while two genuinely different sources still sum.
 *
 * @param value    signed contribution (buffs positive, debuffs negative)
 * @param source   provenance, e.g. {@code buff:Bull's Strength} or {@code feature:<uuid>}
 * @param stackKey identity used for the non-stacking (max, not sum) rule
 */
public record AppliedModifier(int value, String source, String stackKey) {
}
