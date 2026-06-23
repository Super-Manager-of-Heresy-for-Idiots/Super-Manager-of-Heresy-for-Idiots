package com.dnd.app.domain.enums;

/**
 * The role an NPC plays in the world, independent of how it was statted up
 * ({@link NpcSourceType}). Drives the interactions players can have out of combat —
 * notably {@link #MERCHANT}s, who expose a shop the party can buy from and sell to.
 */
public enum NpcRole {
    /** Buys and sells goods: exposes a shop inventory with prices. */
    MERCHANT,
    /** Offers and resolves quests. */
    QUEST_GIVER,
    /** Teaches skills/abilities. */
    TRAINER,
    /** Guards a location; usually not interactive beyond dialogue. */
    GUARD,
    /** Runs an inn/tavern (rest, rumours). */
    INNKEEPER,
    /** Ordinary townsfolk with no special interaction. */
    COMMONER
}
