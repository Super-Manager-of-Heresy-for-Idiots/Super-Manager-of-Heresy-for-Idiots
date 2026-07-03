package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Definition of a feature resource: how its max is computed, how it resets, and its spend cost. */
@Entity
@Table(name = "feature_resource_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureResourceDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "resource_key", nullable = false, length = 64)
    private String resourceKey;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "max_formula_id")
    private UUID maxFormulaId;

    @Column(name = "reset_rest_type_id")
    private UUID resetRestTypeId;

    @Column(name = "reset_event_type_id")
    private UUID resetEventTypeId;

    @Column(name = "reset_amount_formula_id")
    private UUID resetAmountFormulaId;

    @Column(name = "spend_per_use_formula_id")
    private UUID spendPerUseFormulaId;

    @Column(name = "allow_negative", nullable = false)
    @Builder.Default
    private boolean allowNegative = false;

    /** When set, resources sharing this key resolve to one pool per character (e.g. Channel Divinity). */
    @Column(name = "shared_pool_key", length = 64)
    private String sharedPoolKey;
}
