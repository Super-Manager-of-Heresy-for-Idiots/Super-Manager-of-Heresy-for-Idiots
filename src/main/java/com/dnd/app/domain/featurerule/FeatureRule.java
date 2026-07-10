package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс FeatureRule описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRule {

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

    /** {@link FeatureRuleProfile} code. */
    @Column(name = "rule_type", nullable = false, length = 48)
    private String ruleType;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** {@link FeatureReviewStatus} code. */
    @Column(name = "review_status", nullable = false, length = 20)
    @Builder.Default
    private String reviewStatus = FeatureReviewStatus.DRAFT.getCode();

    /** Parser/importer confidence in [0,1]; null for manually authored rules. */
    @Column(name = "confidence")
    private Double confidence;

    /** {@link FeatureRuleSource} code. */
    @Column(nullable = false, length = 16)
    @Builder.Default
    private String source = FeatureRuleSource.MANUAL.getCode();

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(columnDefinition = "text")
    private String notes;

    // ── Versioning (Stage 2): runtime executes ONLY approved_revision_id ──────
    /** The revision currently being edited (may be draft/needs_review; not executed). */
    @Column(name = "current_revision_id")
    private UUID currentRevisionId;

    /** The single approved revision the runtime is allowed to execute; null until approved. */
    @Column(name = "approved_revision_id")
    private UUID approvedRevisionId;

    // ── Game source / ruleset scope (Stage 2) — distinct from {@link #source} (technical) ──
    @Column(name = "ruleset_id")
    private UUID rulesetId;

    @Column(name = "rule_source_id")
    private UUID ruleSourceId;

    /** Scope marker for homebrew packs; no hard FK yet (wired in a later stage). */
    @Column(name = "homebrew_pack_id")
    private UUID homebrewPackId;

    /** Scope marker for campaign-local rules; no hard FK yet (wired in a later stage). */
    @Column(name = "campaign_id")
    private UUID campaignId;

    /** Resolution priority when several scoped rules could apply (higher wins). */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
