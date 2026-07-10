package com.dnd.app.domain.enums;

/**
 * Перечисление NpcRole описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
