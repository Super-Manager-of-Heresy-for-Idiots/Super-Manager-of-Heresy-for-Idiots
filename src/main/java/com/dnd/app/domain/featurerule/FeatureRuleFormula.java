package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A generic named formula attached directly to a feature rule. */
@Entity
@Table(name = "feature_rule_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRuleFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "formula_id", nullable = false)
    private UUID formulaId;

    @Column(name = "formula_key", nullable = false, length = 64)
    private String formulaKey;

    @Column(length = 120)
    private String label;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
