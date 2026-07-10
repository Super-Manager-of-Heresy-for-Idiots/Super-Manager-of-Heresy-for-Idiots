package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс FeatureRuleRevision описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_rule_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRuleRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    /** {@link FeatureReviewStatus} code for this revision. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = FeatureReviewStatus.DRAFT.getCode();

    /** Opaque JSON snapshot of the rule payload at this revision. */
    @Column(name = "rule_payload_snapshot", columnDefinition = "text")
    private String rulePayloadSnapshot;

    @Column(name = "change_reason", columnDefinition = "text")
    private String changeReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "disabled_by")
    private UUID disabledBy;

    @Column(name = "disabled_at")
    private Instant disabledAt;
}
