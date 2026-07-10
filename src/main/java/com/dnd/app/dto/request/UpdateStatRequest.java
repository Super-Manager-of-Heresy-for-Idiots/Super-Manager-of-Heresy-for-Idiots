package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс UpdateStatRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatRequest {

    @NotNull(message = "Значение обязательно")
    @Min(value = 1, message = "Значение характеристики должно быть от 1 до 30")
    @Max(value = 30, message = "Значение характеристики должно быть от 1 до 30")
    private Integer value;
}
