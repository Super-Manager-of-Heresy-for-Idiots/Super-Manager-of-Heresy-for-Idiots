package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A filter selecting a set of spells (class list, school, max level, tag, source) for a spell grant. */
@Entity
@Table(name = "feature_spell_filter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSpellFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "max_spell_level_formula_id")
    private UUID maxSpellLevelFormulaId;

    @Column(length = 32)
    private String tag;

    @Column(name = "source_filter", length = 64)
    private String sourceFilter;
}
