package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Definition of an active effect a feature can apply (duration, stacking, concentration, target). */
@Entity
@Table(name = "feature_effect_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureEffectDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "effect_key", nullable = false, length = 64)
    private String effectKey;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "duration_formula_id")
    private UUID durationFormulaId;

    @Column(name = "duration_unit_id")
    private UUID durationUnitId;

    @Column(name = "concentration_required", nullable = false)
    @Builder.Default
    private boolean concentrationRequired = false;

    /** {@link EffectStackingPolicy} code. */
    @Column(name = "stacking_policy", nullable = false, length = 32)
    @Builder.Default
    private String stackingPolicy = EffectStackingPolicy.STACK.getCode();

    /** Replacement group; effects in the same group can replace each other per policy. */
    @Column(name = "active_effect_group", length = 64)
    private String activeEffectGroup;

    @Column(name = "target_type_id")
    private UUID targetTypeId;
}
