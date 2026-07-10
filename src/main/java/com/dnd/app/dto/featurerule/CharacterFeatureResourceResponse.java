package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterFeatureResourceResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterFeatureResourceResponse {
    private UUID id;
    private UUID resourceDefinitionId;
    private String resourceKey;
    private String displayName;
    private Integer currentValue;
    private Integer maxValue;
    private String sharedPoolKey;
    private boolean allowNegative;
    private Instant lastResetAt;
}
