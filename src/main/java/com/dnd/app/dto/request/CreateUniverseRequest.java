package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CreateUniverseRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUniverseRequest {

    @NotBlank(message = "Слаг вселенной обязателен")
    @Size(max = 60, message = "Слаг не должен превышать 60 символов")
    @Pattern(regexp = "[a-z0-9-]+", message = "Слаг может содержать только строчные латинские буквы, цифры и дефис")
    private String slug;

    @NotBlank(message = "Название вселенной обязательно")
    @Size(max = 120, message = "Название не должно превышать 120 символов")
    private String name;

    private String description;
}
