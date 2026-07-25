package com.dnd.app.domain.enums;

/**
 * Перечисление CharacterQuestStatus описывает индивидуальный статус квеста в журнале
 * персонажа (WORLD_PLAN Этап 2). Не путать с {@link QuestStatus} — мастер-статусом
 * квеста на уровне кампании, которым управляет ГМ.
 */
public enum CharacterQuestStatus {
    ACCEPTED,
    /** Игрок сдал квест квестодателю, награда ждёт подтверждения ГМа (auto_complete_on_turn_in=false). */
    READY_FOR_TURN_IN,
    COMPLETED,
    FAILED,
    ABANDONED
}
