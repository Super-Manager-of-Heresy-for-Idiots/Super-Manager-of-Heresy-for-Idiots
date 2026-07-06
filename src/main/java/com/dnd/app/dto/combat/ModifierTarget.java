package com.dnd.app.dto.combat;

import java.util.UUID;

/**
 * What a modifier lookup is asking for. One point-of-consumption (an ability check, a saving throw,
 * armour class, an attack roll, damage dealt/taken, initiative) so {@code ModifierAggregator} can be
 * the single place that answers "what modifies X for this character?" across every effect source.
 *
 * <p>Stat-scoped kinds ({@link Kind#STAT_CHECK}, {@link Kind#SAVE}, {@link Kind#INITIATIVE}) carry the
 * stat by id and/or slug because callers identify it differently (ability checks by id, initiative by
 * the {@code dex} slug); a source matches if either provided identifier matches. {@link Kind#DAMAGE_TAKEN}
 * carries the incoming damage type for resistance/vulnerability.</p>
 */
public record ModifierTarget(Kind kind, UUID statTypeId, String statSlug, UUID damageTypeId) {

    public enum Kind { STAT_CHECK, SAVE, AC, ATTACK_ROLL, DAMAGE_DEALT, DAMAGE_TAKEN, INITIATIVE }

    public static ModifierTarget statCheck(UUID statTypeId, String statSlug) {
        return new ModifierTarget(Kind.STAT_CHECK, statTypeId, statSlug, null);
    }

    public static ModifierTarget save(UUID statTypeId, String statSlug) {
        return new ModifierTarget(Kind.SAVE, statTypeId, statSlug, null);
    }

    public static ModifierTarget ac() {
        return new ModifierTarget(Kind.AC, null, null, null);
    }

    public static ModifierTarget attackRoll() {
        return new ModifierTarget(Kind.ATTACK_ROLL, null, null, null);
    }

    public static ModifierTarget damageDealt() {
        return new ModifierTarget(Kind.DAMAGE_DEALT, null, null, null);
    }

    public static ModifierTarget damageTaken(UUID damageTypeId) {
        return new ModifierTarget(Kind.DAMAGE_TAKEN, null, null, damageTypeId);
    }

    public static ModifierTarget initiative(String dexSlug) {
        return new ModifierTarget(Kind.INITIATIVE, null, dexSlug, null);
    }
}
