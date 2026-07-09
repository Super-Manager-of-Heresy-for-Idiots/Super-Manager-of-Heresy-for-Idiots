package com.dnd.app.domain.enums;

/**
 * Kind of a {@link com.dnd.app.domain.BattleLog} entry (Phase 1.2). Persisted as a string; the
 * {@code payload} JSON shape depends on the type (e.g. ATTACK carries the roll formula + dice).
 */
public enum BattleLogType {
    ATTACK,
    SAVE,
    DAMAGE,
    HEAL,
    HP_SET,
    TURN,
    ROUND,
    CONDITION,
    EFFECT,
    DEATH_SAVE,
    GM_OVERRIDE,
    ITEM,
    SPELL,
    CONCENTRATION
}
