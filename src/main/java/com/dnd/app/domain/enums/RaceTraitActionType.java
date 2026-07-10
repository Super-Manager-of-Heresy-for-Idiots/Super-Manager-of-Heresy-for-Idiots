package com.dnd.app.domain.enums;

/**
 * Перечисление RaceTraitActionType описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum RaceTraitActionType {
    PASSIVE,
    ACTION,
    BONUS_ACTION,
    REACTION,
    PART_OF_ATTACK_ACTION
}
