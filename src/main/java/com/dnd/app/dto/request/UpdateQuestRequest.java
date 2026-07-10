package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс UpdateQuestRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestRequest {

    @Size(max = 200, message = "Quest title must not exceed 200 characters")
    private String title;

    private String description;

    private String status;

    private Boolean isVisibleToPlayers;
}
