package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс UpdateCampaignBlueprintRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignBlueprintRequest {

    @Size(max = 120, message = "Название не должно превышать 120 символов")
    private String title;

    private String loreDescription;

    private UUID universeId;

    private String coverUrl;

    private Boolean allowForks;
}
