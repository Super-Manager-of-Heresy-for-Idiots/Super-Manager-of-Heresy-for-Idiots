package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result of a resolved attack on the tracker: the roll breakdown, the outcome and the target's
 * HP after damage. {@link #battle} carries the fresh authoritative state so the client can sync
 * its cache without an extra round-trip (WebSocket fan-out still notifies the other participants).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleActionResultResponse {

    private UUID attackerCombatantId;
    private String attackerName;
    private UUID targetCombatantId;
    private String targetName;

    private String attackName;

    /** The effective d20 actually used to resolve the attack (kept for backward compatibility). */
    private Integer d20;

    /** Roll mode used: NORMAL | ADVANTAGE | DISADVANTAGE. */
    private String rollMode;
    /** The two dice for ADVANTAGE/DISADVANTAGE (or the single die under NORMAL in {@code d20A}). */
    private Integer d20A;
    private Integer d20B;
    /** The die selected per the roll mode (max for advantage, min for disadvantage). */
    private Integer effectiveD20;
    /** Frontend-supplied advantage reason, recorded as-is (not validated by core BE this phase). */
    private String advantageReason;

    private Integer attackBonus;
    private Integer total;
    private Integer targetAc;

    /** Save difficulty class for save-based attacks (target rolls a saving throw); null otherwise. */
    private Integer saveDc;

    /** For save-based attacks: the ability the target saves with (e.g. "Ловкость"), its bonus, and the total rolled. */
    private String saveAbility;
    private Integer saveBonus;
    private Integer saveTotal;
    /** Save roll mode used (NORMAL | ADVANTAGE | DISADVANTAGE); null for attack-roll strikes. */
    private String saveRollMode;

    /** HIT | MISS | CRIT for attack rolls; SUCCESS | FAIL for saving throws; ITEM_USED for consumables. */
    private String outcome;

    /** Damage dealt; null on a miss. */
    private Integer damage;
    private String damageType;
    /** How the target's defences changed the damage: NONE | RESISTED | IMMUNE | VULNERABLE; null when not applicable. */
    private String damageModifier;

    /** Chebyshev distance attacker→target in feet, when grid positions were supplied; null otherwise (Phase 2.5). */
    private Integer distanceFt;
    /** Range outcome: IN_REACH | OUT_OF_REACH | IN_RANGE | LONG_RANGE | RANGED_IN_MELEE | BEYOND_LONG_RANGE; null when unchecked (Phase 2.5). */
    private String rangeNote;

    private Integer targetCurrentHp;
    private Integer targetMaxHp;
    private boolean targetDown;

    private BattleResponse battle;
}
