package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс PendingPromptResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPromptResponse {
    private UUID id;
    private UUID combatId;
    private UUID sourceFeatureId;
    private UUID featureTriggerId;
    private UUID triggerEventId;
    private String promptType;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
}
