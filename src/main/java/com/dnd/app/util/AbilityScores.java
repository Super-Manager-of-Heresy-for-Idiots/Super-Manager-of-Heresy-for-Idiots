package com.dnd.app.util;

/** D&D 5e ability scores: a single, correct modifier formula shared across the app. */
public final class AbilityScores {

    private AbilityScores() {}

    /**
     * Ability modifier = floor((score - 10) / 2). Uses {@link Math#floorDiv} so odd scores
     * below 10 round down correctly (9 -> -1), unlike integer {@code /} which truncates toward
     * zero (9 -> 0).
     */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
