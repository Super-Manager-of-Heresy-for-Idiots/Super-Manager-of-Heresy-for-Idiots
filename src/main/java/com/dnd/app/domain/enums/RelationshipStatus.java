package com.dnd.app.domain.enums;

/** Status of a {@code user_relationships} row (one row per normalized user pair). */
public enum RelationshipStatus {
    PENDING,
    FRIENDS,
    BLOCKED
}
