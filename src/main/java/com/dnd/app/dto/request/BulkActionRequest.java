package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс BulkActionRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionRequest {

    /**
     * Перечисление Type описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum Type { DAMAGE, HEAL, CONDITION_ADD, CONDITION_REMOVE }

    @NotEmpty(message = "combatantIds must not be empty")
    private List<UUID> combatantIds;

    @NotNull(message = "type is required")
    private Type type;

    /** Flat amount for DAMAGE / HEAL. */
    private Integer amount;

    /** Damage type for DAMAGE mitigation (resistance/immunity/vulnerability); null = untyped. */
    private UUID damageTypeId;

    /** Optional save-for-half on DAMAGE: each target rolls its own save (server AUTO). */
    private Integer saveDc;
    /** Save ability code for the DAMAGE save (e.g. "dex"). */
    private String saveAbility;
    private Boolean halfOnSave;

    /** Condition for CONDITION_ADD / CONDITION_REMOVE. */
    private UUID conditionId;
    /** Optional source note / duration for CONDITION_ADD. */
    private String sourceText;
    private Integer remainingRounds;
}
