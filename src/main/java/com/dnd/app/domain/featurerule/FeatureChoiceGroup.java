package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A choice a feature rule requires (e.g. Fighting Style, Expertise skills), with N options. */
@Entity
@Table(name = "feature_choice_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureChoiceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "choice_key", nullable = false, length = 64)
    private String choiceKey;

    @Column(name = "min_choices", nullable = false)
    @Builder.Default
    private Integer minChoices = 1;

    /** Formula for the max number of choices; null means max == min. */
    @Column(name = "max_choices_formula_id")
    private UUID maxChoicesFormulaId;

    /** {@link ChoiceTiming} code. */
    @Column(name = "choice_timing", nullable = false, length = 24)
    @Builder.Default
    private String choiceTiming = ChoiceTiming.LEVEL_UP.getCode();

    /** {@link ChoiceReplacePolicy} code. */
    @Column(name = "replace_policy", nullable = false, length = 24)
    @Builder.Default
    private String replacePolicy = ChoiceReplacePolicy.NEVER.getCode();
}
