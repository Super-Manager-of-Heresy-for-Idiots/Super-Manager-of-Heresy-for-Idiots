package com.dnd.app.domain.enums;

/**
 * An opposed melee contest (D&D 5e): GRAPPLE (attacker's Athletics vs the target's Athletics or
 * Acrobatics — on a win the target is grappled, speed 0) and SHOVE (same contest — on a win the
 * target is knocked prone or pushed 5 ft). The defender wins ties.
 */
public enum ContestType {
    GRAPPLE,
    SHOVE
}
