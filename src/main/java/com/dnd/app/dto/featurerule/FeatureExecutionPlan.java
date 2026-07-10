package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureExecutionPlan описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureExecutionPlan {
    private UUID featureId;
    private String featureName;
    private boolean requiresManualAdjudication;
    private List<Damage> damages;
    private List<Healing> healings;
    private List<Resolution> resolutions;
    private List<Attack> attacks;
    /** Area of effect (Phase 2.3); null when the feature/spell has none. Static content data. */
    private Area area;
    /** Lingering zone the cast leaves on the map (Web); null when instant (Fireball). */
    private Zone zone;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Damage {
        private String diceExpression;
        private Integer flatAmount;
        private UUID damageTypeId;
        private boolean requiresAttackHit;
        private boolean requiresSave;
        private boolean halfOnSave;
        private Integer saveDc;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Healing {
        private Integer amount;
        private boolean tempHp;
        private boolean canReviveFromZero;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Resolution {
        private String resolutionType;
        private UUID abilityId;
        private UUID skillId;
        private Integer dc;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Attack {
        private String attackKind;
        private Integer extraAttackCount;
    }

    /** AoE template: SPHERE/CUBE/CONE/CYLINDER/LINE + size in feet (radius / edge / length). */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Area {
        private String shape;
        private Integer sizeFt;
    }

    /** Lingering zone properties: difficult terrain and/or obscurement for the spell's duration. */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Zone {
        private String terrain;
        private String obscurement;
        private boolean persists;
    }
}
