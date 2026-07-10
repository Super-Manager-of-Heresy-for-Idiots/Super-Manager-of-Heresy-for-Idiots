package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SpellGrantAdminResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellGrantAdminResponse {
    private UUID id;
    private UUID spellId;
    private boolean countsAgainstKnown;
    private boolean alwaysPrepared;
    private boolean castWithoutSlot;
    private UUID spellcastingAbilityOverrideId;
}
