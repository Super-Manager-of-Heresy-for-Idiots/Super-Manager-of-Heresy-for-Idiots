package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс AddEnchantmentRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEnchantmentRequest {

    @NotNull(message = "enchantmentTypeId обязателен")
    private UUID enchantmentTypeId;

    @Size(max = 255, message = "Заметка не должна превышать 255 символов")
    private String notes;
}
