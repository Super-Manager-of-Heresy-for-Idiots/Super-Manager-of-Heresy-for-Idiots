package com.dnd.app.domain.enums;

/**
 * How an attack's d20 is rolled. ADVANTAGE rolls two dice and keeps the higher, DISADVANTAGE the
 * lower; NORMAL is a single die. Virtual dice apply this directly; manual dice supply {@code d20A}
 * and {@code d20B} (or a single legacy {@code d20} for NORMAL).
 */
public enum AttackRollMode {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE
}
