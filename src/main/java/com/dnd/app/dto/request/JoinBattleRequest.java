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
 * Класс JoinBattleRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinBattleRequest {

    @NotEmpty(message = "At least one character is required")
    @Valid
    private List<CharacterJoin> characters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterJoin {

        @NotNull(message = "Character ID is required")
        private UUID characterId;

        @Min(value = 1, message = "d20 must be between 1 and 20")
        @Max(value = 20, message = "d20 must be between 1 and 20")
        private Integer d20;
    }
}
