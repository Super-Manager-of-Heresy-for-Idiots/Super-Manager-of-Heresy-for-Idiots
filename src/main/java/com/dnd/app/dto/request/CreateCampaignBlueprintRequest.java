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
 * Класс CreateCampaignBlueprintRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignBlueprintRequest {

    @NotBlank(message = "Название шаблона кампании обязательно")
    @Size(max = 120, message = "Название не должно превышать 120 символов")
    private String title;

    private String loreDescription;

    @NotNull(message = "Вселенная обязательна")
    private UUID universeId;

    private String coverUrl;

    private Boolean allowForks;
}
