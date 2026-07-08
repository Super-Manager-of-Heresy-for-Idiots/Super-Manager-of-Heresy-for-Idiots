package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.enums.CombatantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Applies a target's damage resistance / immunity / vulnerability to a typed damage total — the one
 * place combat consults these, so both characters and monsters are handled consistently (BTL-07).
 *
 * <ul>
 *   <li><b>Character</b> targets: from active feature effects, legacy buffs and species traits via
 *       {@link ModifierAggregator#damageMultiplier} (0.0 = immune, 0.5 = resist, 2.0 = vulnerable).</li>
 *   <li><b>Monster</b> targets: innate resistances/immunities/vulnerabilities authored on the bestiary
 *       statblock. These were previously ignored entirely (the old resistance step was a character-only
 *       no-op for monsters).</li>
 * </ul>
 *
 * <p><b>Untyped damage</b> (no {@code damageTypeId}) is never modified — this is a deliberate rule, not
 * a gap: effects that deal generic/untyped damage bypass elemental resistances by design.
 */
@Service
@RequiredArgsConstructor
public class DamageMitigationService {

    private final ModifierAggregator modifierAggregator;

    /** How the target's defences changed the incoming damage — surfaced in the attack log/UI. */
    public enum DamageModifier {
        NONE,
        RESISTED,
        IMMUNE,
        VULNERABLE
    }

    /** The mitigated damage and which modifier (if any) was applied. */
    public record Mitigation(int finalDamage, DamageModifier modifier) {
    }

    /**
     * Mitigate a positive, typed {@code damage} total against {@code target}. Non-positive damage and
     * untyped damage ({@code damageTypeId == null}) pass through unchanged as {@link DamageModifier#NONE}.
     */
    public Mitigation mitigate(BattleCombatant target, int damage, UUID damageTypeId) {
        if (target == null || damage <= 0 || damageTypeId == null) {
            return new Mitigation(Math.max(0, damage), DamageModifier.NONE);
        }
        DamageModifier modifier = resolveModifier(target, damageTypeId);
        int finalDamage = switch (modifier) {
            case IMMUNE -> 0;
            case RESISTED -> damage / 2; // floor(x / 2)
            case VULNERABLE -> damage * 2;
            case NONE -> damage;
        };
        return new Mitigation(finalDamage, modifier);
    }

    private DamageModifier resolveModifier(BattleCombatant target, UUID damageTypeId) {
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null) {
            Monster monster = target.getMonster();
            if (immuneTo(monster, damageTypeId)) {
                return DamageModifier.IMMUNE;
            }
            boolean resist = resistantTo(monster, damageTypeId);
            boolean vulnerable = vulnerableTo(monster, damageTypeId);
            if (resist && !vulnerable) {
                return DamageModifier.RESISTED;
            }
            if (vulnerable && !resist) {
                return DamageModifier.VULNERABLE;
            }
            return DamageModifier.NONE;
        }
        if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null) {
            double multiplier = modifierAggregator.damageMultiplier(target.getCharacter().getId(), damageTypeId);
            if (multiplier == 0.0) {
                return DamageModifier.IMMUNE;
            }
            if (multiplier < 1.0) {
                return DamageModifier.RESISTED;
            }
            if (multiplier > 1.0) {
                return DamageModifier.VULNERABLE;
            }
            return DamageModifier.NONE;
        }
        return DamageModifier.NONE;
    }

    private static boolean immuneTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageImmunities().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }

    private static boolean resistantTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageResistances().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }

    private static boolean vulnerableTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageVulnerabilities().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }
}
