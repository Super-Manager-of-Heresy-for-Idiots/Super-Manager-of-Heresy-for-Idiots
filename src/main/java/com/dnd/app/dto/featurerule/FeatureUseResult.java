package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс FeatureUseResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUseResult {
    private UUID featureId;
    private String featureName;
    private String actionType;
    private String resourceKey;
    private Integer resourceSpent;
    private Integer resourceRemaining;
    private UUID logId;
    private String message;
}
