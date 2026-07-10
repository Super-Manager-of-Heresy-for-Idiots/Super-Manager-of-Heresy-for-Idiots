package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CompleteQuestRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteQuestRequest {

    @NotNull(message = "Recipient character ID is required")
    private UUID recipientCharacterId;

    /**
     * Optional XP override applied at the moment of completion. When provided,
     * this total replaces the sum of XP defined on the quest's reward entries;
     * when null, the reward entries' XP is used.
     */
    @Min(value = 0, message = "XP amount must not be negative")
    private Long xpAmount;
}
