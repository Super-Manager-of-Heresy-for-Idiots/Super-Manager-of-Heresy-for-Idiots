package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CreateCharacterRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterRequest {

    @NotBlank(message = "Имя персонажа обязательно")
    @Size(max = 100, message = "Имя персонажа не должно превышать 100 символов")
    private String name;

    @NotNull(message = "ID класса обязателен")
    private UUID classId;

    @NotNull(message = "ID расы обязателен")
    private UUID raceId;

    private UUID selectedLineageId;

    @NotNull(message = "ID кампании обязателен")
    private UUID campaignId;
}
