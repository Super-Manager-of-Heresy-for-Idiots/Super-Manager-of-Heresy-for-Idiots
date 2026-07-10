package com.dnd.app.domain.enums;

/**
 * Перечисление QuestStatus описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum QuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED,
    HIDDEN,
    ARCHIVED
}
