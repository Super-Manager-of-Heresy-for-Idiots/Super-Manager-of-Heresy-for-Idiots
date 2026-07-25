package com.dnd.app.domain.enums;

/**
 * Перечисление CampParticipantState описывает состояние участника привала по отдыху.
 * Отдых применяется per-character, поэтому у каждого участника собственное состояние:
 * ошибка одного не откатывает остальных.
 */
public enum CampParticipantState {
    /** Отдых ещё не применялся. */
    NOT_RESTED,
    /** Транзакция отдыха выполняется. */
    RESTING,
    /** Отдых применён полностью. */
    RESTED,
    /** Мастер засчитал частичный отдых после прерывания. */
    PARTIAL,
    /** Транзакция отдыха отклонена — причина в rest_error_code/rest_error_message. */
    FAILED
}
