package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс SpendActionRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendActionRequest {

    @NotNull(message = "Slot is required")
    private Slot slot;

    /**
     * Перечисление Slot описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum Slot {
        ACTION, BONUS_ACTION, LEGENDARY_ACTION, REACTION
    }
}
