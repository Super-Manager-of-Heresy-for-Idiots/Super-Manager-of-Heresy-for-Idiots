package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterActiveEffectResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterActiveEffectResponse {
    private UUID id;
    private UUID buffDebuffId;
    private String buffDebuffName;
    private boolean isBuff;
    private String effectType;
    private Integer modifierValue;
    private String targetStatName;
    private Integer remainingRounds;
    private Instant appliedAt;
    private String appliedByUsername;
}
