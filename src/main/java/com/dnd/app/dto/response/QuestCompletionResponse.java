package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс QuestCompletionResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestCompletionResponse {

    private UUID questId;
    private String status;
    private UUID recipientCharacterId;
    private String recipientCharacterName;
    private int itemsGranted;
    private long xpGranted;
}
