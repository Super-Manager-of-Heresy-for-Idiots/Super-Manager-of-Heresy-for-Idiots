package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A durable prompt for an optional trigger/reaction, surviving client reload and backend restart. */
@Entity
@Table(name = "pending_gameplay_prompt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingGameplayPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "combat_id")
    private UUID combatId;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "source_feature_id")
    private UUID sourceFeatureId;

    @Column(name = "trigger_event_id")
    private UUID triggerEventId;

    @Column(name = "feature_trigger_id")
    private UUID featureTriggerId;

    @Column(name = "prompt_type", length = 32)
    private String promptType;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /** pending | resolved | declined | expired. */
    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "pending";

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
