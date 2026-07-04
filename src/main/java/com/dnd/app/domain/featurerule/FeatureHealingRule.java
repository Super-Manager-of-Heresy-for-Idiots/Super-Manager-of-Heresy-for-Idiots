package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Structured healing / temp HP output of a feature. */
@Entity
@Table(name = "feature_healing_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureHealingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "amount_formula_id")
    private UUID amountFormulaId;

    @Column(name = "target_type_id")
    private UUID targetTypeId;

    @Column(name = "is_temp_hp", nullable = false)
    @Builder.Default
    private boolean tempHp = false;

    @Column(name = "can_revive_from_zero", nullable = false)
    @Builder.Default
    private boolean canReviveFromZero = false;
}
