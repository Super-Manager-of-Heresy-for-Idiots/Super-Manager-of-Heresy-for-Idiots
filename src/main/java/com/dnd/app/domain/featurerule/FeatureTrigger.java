package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Binds a feature rule to a gameplay event, with a predicate and reaction/resource cost. */
@Entity
@Table(name = "feature_trigger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "event_type_id")
    private UUID eventTypeId;

    /** before | after | replace | interrupt. */
    @Column(length = 16)
    private String timing;

    @Column(name = "predicate_formula_id")
    private UUID predicateFormulaId;

    @Column(name = "requires_player_confirmation", nullable = false)
    @Builder.Default
    private boolean requiresPlayerConfirmation = true;

    @Column(name = "consumes_reaction", nullable = false)
    @Builder.Default
    private boolean consumesReaction = true;

    @Column(name = "consumes_resource_definition_id")
    private UUID consumesResourceDefinitionId;
}
