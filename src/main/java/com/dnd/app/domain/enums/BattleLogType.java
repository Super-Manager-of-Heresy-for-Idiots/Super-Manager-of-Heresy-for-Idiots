package com.dnd.app.domain.enums;

/**
 * Перечисление BattleLogType описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
    CONCENTRATION,
    STANDARD_ACTION,
    CONTEST,
    FORCED_MOVE,
    TELEPORT,
    TRAP,
    FALL,
    UNDO,
    SURPRISE,
    READY
}
