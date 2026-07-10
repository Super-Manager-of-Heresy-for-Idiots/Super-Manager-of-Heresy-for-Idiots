package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс ActiveEffectResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveEffectResponse {
    private UUID id;
    private UUID effectDefinitionId;
    private String effectKey;
    private String displayName;
    private UUID sourceFeatureId;
    private UUID sourceCharacterId;
    private Instant startedAt;
    private Instant expiresAt;
    private Integer remainingRounds;
    private String status;
    private boolean concentrationRequired;
    private String stackingPolicy;
    private String activeEffectGroup;
    private List<Modifier> modifiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Modifier {
        private UUID id;
        private String modifierType;
        private UUID valueFormulaId;
        private UUID abilityId;
        private UUID skillId;
        private UUID damageTypeId;
        private UUID conditionId;
    }
}
