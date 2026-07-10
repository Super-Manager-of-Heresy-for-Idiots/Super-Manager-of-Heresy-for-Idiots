package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ActiveEffectEditRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveEffectEditRequest {

    @NotBlank
    @Size(max = 64)
    private String effectKey;

    @Size(max = 120)
    private String displayName;

    /** DSL duration expression (e.g. {@code 1+floor(class_level("wizard")/2)}); blank = none. */
    @Size(max = 2000)
    private String durationFormula;

    private UUID durationUnitId;
    private boolean concentrationRequired;

    /** {@code stack} | {@code replace_same_feature} | {@code replace_same_group} | {@code highest_only}. */
    @Size(max = 32)
    private String stackingPolicy;

    @Size(max = 64)
    private String activeEffectGroup;

    private UUID targetTypeId;

    private List<Modifier> modifiers;
    private List<EndCondition> endConditions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Modifier {
        @Size(max = 32)
        private String modifierType;
        @Size(max = 2000)
        private String valueFormula;
        private UUID damageTypeId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndCondition {
        private UUID triggerEventTypeId;
        private boolean sameFeatureReuse;
        private UUID restTypeId;
        @Size(max = 2000)
        private String predicateFormula;
    }
}
