package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс FeatureSpellCastResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureSpellCastResult {
    private UUID spellGrantId;
    private UUID spellId;
    private boolean castWithoutSlot;
    private String resourceKey;
    private Integer resourceSpent;
    private Integer resourceRemaining;
    private String message;
}
