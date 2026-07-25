package com.dnd.app.domain.enums;

/**
 * Перечисление RollPromptStatus описывает жизненный цикл запрошенной проверки (ROLL_PROMPT):
 * создана мастером, брошена игроком либо отменена мастером.
 */
public enum RollPromptStatus {
    PENDING,
    ROLLED,
    CANCELLED
}
