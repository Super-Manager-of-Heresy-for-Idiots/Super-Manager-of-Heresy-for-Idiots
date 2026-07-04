package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A way an effect can end: on an event, a condition, feature reuse, a rest, or a predicate. */
@Entity
@Table(name = "feature_effect_end_condition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureEffectEndCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "effect_definition_id", nullable = false)
    private UUID effectDefinitionId;

    @Column(name = "trigger_event_type_id")
    private UUID triggerEventTypeId;

    @Column(name = "condition_id")
    private UUID conditionId;

    @Column(name = "same_feature_reuse", nullable = false)
    @Builder.Default
    private boolean sameFeatureReuse = false;

    @Column(name = "rest_type_id")
    private UUID restTypeId;

    @Column(name = "predicate_formula_id")
    private UUID predicateFormulaId;
}
