package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureChoiceSummary описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureChoiceSummary {
    private UUID id;
    private UUID featureRuleId;
    private String choiceKey;
    private Integer minChoices;
    private UUID maxChoicesFormulaId;
    private String choiceTiming;
    private String replacePolicy;
    private List<Option> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private UUID id;
        private String optionType;
        private UUID targetEntityId;
        private UUID filterRuleId;
        private Integer sortOrder;
    }
}
