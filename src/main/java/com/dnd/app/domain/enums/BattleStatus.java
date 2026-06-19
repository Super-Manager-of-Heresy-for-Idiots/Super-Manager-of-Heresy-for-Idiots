package com.dnd.app.domain.enums;

/**
 * Lifecycle of a campaign battle.
 *
 * <ul>
 *   <li>{@code ASSEMBLING} — the GM is building the monster group; preview (danger / xp) is
 *       available, initiative is not yet rolled and players are not yet involved.</li>
 *   <li>{@code ACTIVE} — initiative has been rolled, the tracker is live and players can join
 *       their characters and take turns.</li>
 *   <li>{@code COMPLETED} — the fight is over; the tracker is frozen for reference.</li>
 * </ul>
 */
public enum BattleStatus {
    ASSEMBLING,
    ACTIVE,
    COMPLETED
}
