package com.dnd.app.domain.enums;

/**
 * Перечисление CharacterQuestStatus описывает индивидуальный статус квеста в журнале
 * персонажа (WORLD_PLAN Этап 2). Не путать с {@link QuestStatus} — мастер-статусом
 * квеста на уровне кампании, которым управляет ГМ.
 */
public enum CharacterQuestStatus {
    ACCEPTED,
    COMPLETED,
    FAILED,
    ABANDONED
}
