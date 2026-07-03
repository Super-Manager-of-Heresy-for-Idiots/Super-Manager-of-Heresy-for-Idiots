package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Per-character live state of a feature resource (current/max, last reset). */
@Entity
@Table(name = "character_feature_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterFeatureResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "resource_definition_id", nullable = false)
    private UUID resourceDefinitionId;

    /** Copied from the definition; dedups shared pools to one row per character. */
    @Column(name = "shared_pool_key", length = 64)
    private String sharedPoolKey;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Integer currentValue = 0;

    @Column(name = "max_value_snapshot")
    private Integer maxValueSnapshot;

    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
