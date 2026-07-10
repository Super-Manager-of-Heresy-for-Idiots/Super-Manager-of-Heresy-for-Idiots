package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс RaceSpeedDto описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSpeedDto {

    @NotNull(message = "Walk speed is required")
    @Min(value = 0, message = "Walk speed must be >= 0")
    private Integer walk;

    @Min(value = 0, message = "Fly speed must be >= 0")
    private Integer fly;

    @Min(value = 0, message = "Swim speed must be >= 0")
    private Integer swim;

    @Min(value = 0, message = "Climb speed must be >= 0")
    private Integer climb;

    @Min(value = 0, message = "Burrow speed must be >= 0")
    private Integer burrow;
}
