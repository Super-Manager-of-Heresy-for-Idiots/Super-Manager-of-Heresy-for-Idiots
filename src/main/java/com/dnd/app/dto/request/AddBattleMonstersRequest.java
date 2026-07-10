package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс AddBattleMonstersRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBattleMonstersRequest {

    @NotEmpty(message = "At least one monster entry is required")
    @Valid
    private List<MonsterEntry> monsters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonsterEntry {

        @NotNull(message = "Monster ID is required")
        private UUID monsterId;

        @NotNull(message = "Count is required")
        @Min(value = 1, message = "Count must be at least 1")
        @Max(value = 50, message = "Count must be at most 50")
        private Integer count;
    }
}
