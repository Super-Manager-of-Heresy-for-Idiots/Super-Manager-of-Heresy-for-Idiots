package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.StandardActionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс StandardActionRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardActionRequest {

    @NotNull(message = "Standard action type is required")
    private StandardActionType type;

    /** Action-economy slot to spend. Null → ACTION. */
    private SpendActionRequest.Slot slot;

    /** The aided ally for HELP. Required for HELP, ignored otherwise. */
    private UUID targetCombatantId;

    /** HIDE: manual Stealth d20; omit to have the server roll it. */
    @Min(value = 1, message = "stealthD20 must be between 1 and 20")
    @Max(value = 20, message = "stealthD20 must be between 1 and 20")
    private Integer stealthD20;

    /** HIDE: the actor's Stealth modifier (added to the d20). Null → 0. */
    private Integer stealthBonus;

    /** HIDE: contest DC (highest enemy passive Perception). Null → auto-succeed (GM adjudicates). */
    private Integer hideDc;
}
