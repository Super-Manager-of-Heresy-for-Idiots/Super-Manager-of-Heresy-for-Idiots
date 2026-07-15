package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс FeatureSpellGrantResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureSpellGrantResponse {
    private UUID id;
    private UUID featureId;
    private String featureName;
    private UUID spellId;
    private boolean countsAgainstKnown;
    private boolean alwaysPrepared;
    private boolean castWithoutSlot;
    private UUID usesResourceDefinitionId;
    private UUID spellcastingAbilityOverrideId;
    private Filter filter;
    /** Источник гранта: {@code "class"} (классовая фича) или {@code "item"} (предмет). */
    private String source;
    /** Для item-грантов — id экземпляра предмета, дающего заклинание (иначе null). */
    private UUID sourceItemInstanceId;
    /** Для item-грантов — отображаемое имя предмета (иначе null). */
    private String sourceItemName;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Filter {
        private UUID classId;
        private UUID schoolId;
        private Integer maxSpellLevel;
        private String tag;
        private String sourceFilter;
    }
}
