package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A tracked problem with a class feature or one of its rules.
 *
 * <p>Replaces {@code class_feature.is_warning} as the primary tracking model: severity, type, the
 * source text fragment that triggered it, and resolution state. An unresolved {@code error} issue
 * blocks approval of the related rule.</p>
 */
@Entity
@Table(name = "feature_rule_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRuleIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** {@link FeatureRuleOwnerType} code. Only {@code CLASS_FEATURE} is used today. */
    @Column(name = "owner_type", nullable = false, length = 32)
    @Builder.Default
    private String ownerType = FeatureRuleOwnerType.CLASS_FEATURE.getCode();

    /** Id of the owning entity; for {@code CLASS_FEATURE} this is {@code class_feature.class_feature_id}. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Optional link to the specific rule this issue is about; null for feature-level issues. */
    @Column(name = "feature_rule_id")
    private UUID featureRuleId;

    /** {@code rule_issue_type} code. */
    @Column(name = "issue_type", nullable = false, length = 48)
    private String issueType;

    /** {@link FeatureIssueSeverity} code. */
    @Column(nullable = false, length = 16)
    private String severity;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "source_text_fragment", columnDefinition = "text")
    private String sourceTextFragment;

    @Column(nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
