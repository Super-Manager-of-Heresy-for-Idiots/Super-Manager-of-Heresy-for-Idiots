package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an opposed Grapple/Shove contest (Phase 2.7): both totals, who won, and the condition
 * applied on a win. {@link #battle} carries the fresh authoritative state for cache sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestResultResponse {

    private String type;
    private String attackerName;
    private String targetName;
    private int attackerRoll;
    private int attackerTotal;
    private int targetRoll;
    private int targetTotal;
    private boolean attackerWins;
    /** Condition applied to the target on a win ("grappled" | "prone"); null on a loss or a PUSH shove. */
    private String condition;

    private BattleResponse battle;
}
