package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Audit entry for a single feature use (Stage 6). Superseded/augmented by the gameplay event bus in Stage 11. */
@Entity
@Table(name = "feature_use_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureUseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "feature_id")
    private UUID featureId;

    @Column(name = "feature_rule_id")
    private UUID featureRuleId;

    @Column(name = "combat_id")
    private UUID combatId;

    @Column(name = "action_type", length = 24)
    private String actionType;

    @Column(name = "resource_spent")
    private Integer resourceSpent;

    @Column(columnDefinition = "text")
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
