package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentCharacterClass;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "custom_resource_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomResourceType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "max_value")
    private Integer maxValue;

    /** Bounded-DSL formula for the max, evaluated per character (e.g. {@code class_level("monk")}); null = fixed max_value. */
    @Column(name = "max_formula", columnDefinition = "text")
    private String maxFormula;

    /** Rest that refills this resource to its max: {@code none} | {@code short_rest} | {@code long_rest}. Deprecated — superseded by the per-window recovery fields; kept for back-compat. */
    @Column(name = "reset_on", nullable = false, length = 16)
    @Builder.Default
    private String resetOn = "none";

    /** How much this resource recovers on a short rest: {@code none} | {@code full} | {@code formula}. */
    @Column(name = "short_rest_recovery", nullable = false, length = 16)
    @Builder.Default
    private String shortRestRecovery = "none";

    /** DSL formula for the number of charges restored on a short rest, when {@code shortRestRecovery=formula}. */
    @Column(name = "short_rest_formula", columnDefinition = "text")
    private String shortRestFormula;

    /** How much this resource recovers on a long rest: {@code none} | {@code full} | {@code formula}. */
    @Column(name = "long_rest_recovery", nullable = false, length = 16)
    @Builder.Default
    private String longRestRecovery = "none";

    /** DSL formula for the number of charges restored on a long rest, when {@code longRestRecovery=formula}. */
    @Column(name = "long_rest_formula", columnDefinition = "text")
    private String longRestFormula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_bound_id")
    private ContentCharacterClass classBound;

    /** Feat this resource is granted by (e.g. Lucky → Luck Points); null = not feat-granted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_bound_id")
    private Feat featBound;
}
