package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Attack-economy rule (Extra Attack count, weapon filter, damage override). */
@Entity
@Table(name = "feature_attack_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureAttackRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** weapon | spell | unarmed | special. */
    @Column(name = "attack_kind", nullable = false, length = 24)
    private String attackKind;

    @Column(name = "extra_attack_count_formula_id")
    private UUID extraAttackCountFormulaId;

    @Column(name = "allowed_weapon_filter_id")
    private UUID allowedWeaponFilterId;

    @Column(name = "damage_override_rule_id")
    private UUID damageOverrideRuleId;
}
