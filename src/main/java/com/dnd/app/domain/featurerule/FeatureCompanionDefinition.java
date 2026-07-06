package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Rule-authored definition for companions/summons before character-specific instances are created. */
@Entity
@Table(name = "feature_companion_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureCompanionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "companion_key", nullable = false, length = 64)
    private String companionKey;

    /** Bestiary monster id, validated at the application boundary when concrete content is selected. */
    @Column(name = "monster_id")
    private UUID monsterId;

    @Column(name = "name_template", length = 120)
    private String nameTemplate;

    @Column(name = "hp_formula_id")
    private UUID hpFormulaId;

    @Column(name = "ac_formula_id")
    private UUID acFormulaId;

    @Column(name = "attack_bonus_formula_id")
    private UUID attackBonusFormulaId;

    @Column(name = "summon_timing", length = 32)
    private String summonTiming;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(columnDefinition = "text")
    private String notes;
}
