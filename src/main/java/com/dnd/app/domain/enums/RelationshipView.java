package com.dnd.app.domain.enums;

/**
 * Перечисление RelationshipView описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum RelationshipView {
    NONE,
    PENDING_OUTGOING,
    PENDING_INCOMING,
    FRIENDS,
    BLOCKED
}
