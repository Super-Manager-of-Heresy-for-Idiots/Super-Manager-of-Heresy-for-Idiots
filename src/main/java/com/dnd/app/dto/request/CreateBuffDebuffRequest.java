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
 * Класс CreateBuffDebuffRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuffDebuffRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
    private String name;

    private String description;

    @NotBlank(message = "Тип эффекта обязателен")
    @Size(max = 30, message = "Тип эффекта не должен превышать 30 символов")
    private String effectType;

    private UUID targetStatId;

    private Integer modifierValue;

    private Integer durationRounds;

    @NotNull(message = "Поле isBuff обязательно")
    private Boolean isBuff;
}
