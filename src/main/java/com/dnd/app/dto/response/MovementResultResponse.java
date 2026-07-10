package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс MovementResultResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResultResponse {

    /** Whether the spend was committed (true) or refused with no state change (false). */
    private boolean allowed;

    /** null when allowed; otherwise NOT_ACTIVE_TURN | MOVEMENT_BUDGET_EXCEEDED | BATTLE_NOT_ACTIVE. */
    private String reason;

    /** Feet of movement left this turn after this call; may be negative under a GM override. */
    private int remainingFt;

    /** The combatant's total movement speed this turn, in feet. */
    private int speedFt;

    /** True if the requested feet fit within the budget on an active turn, regardless of override. */
    private boolean withinBudget;

    /** True if checks were skipped because a GM explicitly overrode the rules (logged by map). */
    private boolean gmOverride;
}
