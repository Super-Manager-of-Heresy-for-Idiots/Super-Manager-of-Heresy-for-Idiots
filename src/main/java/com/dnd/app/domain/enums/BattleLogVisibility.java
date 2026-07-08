package com.dnd.app.domain.enums;

/**
 * Who may read a {@link com.dnd.app.domain.BattleLog} entry (Phase 1.2). {@code GM_ONLY} entries are
 * filtered out of the log API for non-GM callers (e.g. private death-save pips).
 */
public enum BattleLogVisibility {
    PUBLIC,
    GM_ONLY
}
