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
    private Integer d20;
    private Integer attackBonus;
    private Integer total;
    private Integer targetAc;

    /** Save difficulty class for save-based attacks (target rolls a saving throw); null otherwise. */
    private Integer saveDc;

    /** HIT | MISS | CRIT for attack rolls; SUCCESS | FAIL for saving throws; ITEM_USED for consumables. */
    private String outcome;

    /** Damage dealt; null on a miss. */
    private Integer damage;
    private String damageType;

    private Integer targetCurrentHp;
    private Integer targetMaxHp;
    private boolean targetDown;

    private BattleResponse battle;
}
