package com.dnd.app.domain.enums;

/**
 * Cover the target benefits from (D&D 5e). HALF gives +2, THREE_QUARTERS +5 to the target's AC and
 * Dexterity saving throws; TOTAL cannot be targeted directly. Chosen manually by the attacker in
 * this phase (automatic line-of-sight evaluation from walls is a later map concern).
 */
public enum CoverType {
    NONE(0),
    HALF(2),
    THREE_QUARTERS(5),
    TOTAL(0);

    private final int bonus;

    CoverType(int bonus) {
        this.bonus = bonus;
    }

    /** AC / Dex-save bonus this cover grants the target (0 for NONE and TOTAL — TOTAL blocks targeting entirely). */
    public int bonus() {
        return bonus;
    }
}
