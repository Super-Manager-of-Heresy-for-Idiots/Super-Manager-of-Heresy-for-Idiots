package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Filter constraining which monsters a feature (e.g. Wild Shape) may transform into. */
@Entity
@Table(name = "feature_allowed_monster_filter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureAllowedMonsterFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** Creature type code (e.g. {@code beast}); matched against the monster's creature types. */
    @Column(name = "creature_type", length = 32)
    private String creatureType;

    @Column(name = "max_cr_formula_id")
    private UUID maxCrFormulaId;

    @Column(name = "movement_restriction", length = 24)
    private String movementRestriction;

    @Column(name = "size_filter", length = 24)
    private String sizeFilter;

    @Column(name = "source_filter", length = 64)
    private String sourceFilter;
}
