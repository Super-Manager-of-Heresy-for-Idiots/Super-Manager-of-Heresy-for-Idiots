package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс EffectMetadataResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectMetadataResponse {
    private List<RuleRefOption> durationUnits;
    private List<RuleRefOption> stackingPolicies;
    private List<RuleRefOption> targetTypes;
    private List<RuleRefOption> restTypes;
    private List<RuleRefOption> triggerEventTypes;
}
