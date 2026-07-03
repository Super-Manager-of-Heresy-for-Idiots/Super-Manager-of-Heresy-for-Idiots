package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored, bounded DSL formula (resource max, duration, DC, dice scaling, eligibility predicate, …).
 * Evaluated by {@code FeatureFormulaEvaluator} against a typed context; no Java/reflection access.
 */
@Entity
@Table(name = "feature_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String expression;

    /** scalar | predicate | dice — intended usage. */
    @Column(name = "expression_type", length = 24)
    private String expressionType;

    /** {@link FormulaResultType} code. */
    @Column(name = "result_type", nullable = false, length = 16)
    private String resultType;

    /** {@link FormulaRoundingMode} code. */
    @Column(name = "rounding_mode", nullable = false, length = 16)
    @Builder.Default
    private String roundingMode = FormulaRoundingMode.NONE.getCode();

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    /** JSON array of context variable/function names the expression references. */
    @Column(name = "context_requirements", columnDefinition = "text")
    private String contextRequirements;

    /** valid | invalid | unknown. */
    @Column(name = "validation_status", nullable = false, length = 16)
    @Builder.Default
    private String validationStatus = "unknown";

    @Column(name = "validation_message", columnDefinition = "text")
    private String validationMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
