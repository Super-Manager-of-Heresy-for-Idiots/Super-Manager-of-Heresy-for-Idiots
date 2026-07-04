package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * The structured combat resolution a feature would produce for its actor: what to roll (dice, DC),
 * what damage/healing to apply, and any attack-economy effect. Computed from the feature's rules +
 * the actor's formula context. The actual dice rolls happen at the client/GM; the runtime supplies
 * the structure and numbers.
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
}
