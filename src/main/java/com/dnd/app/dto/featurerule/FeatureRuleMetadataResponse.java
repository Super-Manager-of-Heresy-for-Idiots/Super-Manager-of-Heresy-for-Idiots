package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс FeatureRuleMetadataResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleMetadataResponse {
    private List<CodeLabel> ruleTypes;
    private List<CodeLabel> reviewStatuses;
    private List<CodeLabel> severities;
    private List<CodeLabel> issueTypes;
    private List<CodeLabel> sources;
}
