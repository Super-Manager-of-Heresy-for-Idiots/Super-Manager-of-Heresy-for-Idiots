package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс InitiativeOrderRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeOrderRequest {

    @NotEmpty(message = "entries must not be empty")
    @Size(max = 500, message = "at most 500 entries are allowed")
    @Valid
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        @NotNull(message = "combatantId is required")
        private UUID combatantId;

        @NotNull(message = "initiative is required")
        @Min(value = -100, message = "initiative must be >= -100")
        @Max(value = 100, message = "initiative must be <= 100")
        private Integer initiative;
    }
}
