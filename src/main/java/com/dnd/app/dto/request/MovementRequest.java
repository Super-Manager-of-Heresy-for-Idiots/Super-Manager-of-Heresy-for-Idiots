package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс MovementRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementRequest {

    @NotNull(message = "combatantId is required")
    private UUID combatantId;

    @NotNull(message = "feet is required")
    @PositiveOrZero(message = "feet must be zero or positive")
    private Integer feet;

    /** GM explicitly moving a token outside the rules; skips checks but is flagged in the result. */
    private boolean gmOverride;

    /** Idempotency key for the move command (dedup wired in Phase 2.14). */
    private UUID clientCommandId;

    /**
     * Режим движения (фаза 2.11): WALK/FLY/SWIM/CLIMB/BURROW. Определяет, какая скорость статблока
     * используется как бюджет перемещения. {@code null} трактуется как ходьба.
     */
    private String mode;
}
