package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс FeatureApplyResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureApplyResult {
    private UUID targetCharacterId;
    private Integer damageApplied;
    private Integer healingApplied;
    private Integer targetCurrentHp;
    private Integer targetMaxHp;
}
