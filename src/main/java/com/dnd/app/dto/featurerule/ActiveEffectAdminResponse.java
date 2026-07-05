package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** The full ACTIVE_EFFECT graph for one feature rule: definition + stat modifiers + end conditions. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveEffectAdminResponse {
    private UUID definitionId;
    private String effectKey;
    private String displayName;
    private String durationFormula;
    private String durationFormulaStatus;
    private String durationFormulaMessage;
    private UUID durationUnitId;
    private boolean concentrationRequired;
    private String stackingPolicy;
    private String activeEffectGroup;
    private UUID targetTypeId;
    private List<Modifier> modifiers;
    private List<EndCondition> endConditions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Modifier {
        private UUID id;
        private String modifierType;
        private String valueFormula;
        private String valueFormulaStatus;
        private String valueFormulaMessage;
        private UUID damageTypeId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndCondition {
        private UUID id;
        private UUID triggerEventTypeId;
        private boolean sameFeatureReuse;
        private UUID restTypeId;
        private String predicateFormula;
        private String predicateFormulaStatus;
        private String predicateFormulaMessage;
    }
}
