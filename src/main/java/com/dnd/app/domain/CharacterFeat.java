package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A feat a character has (S1, FEAT owner). Structured record replacing the free-text
 * {@code character.features} blob for feats — unblocks feature-rule owner_type=FEAT and
 * auto-provisioning of feat-bound resources. Unique per (character, feat).
 */
@Entity
@Table(name = "character_feats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterFeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "feat_id", nullable = false)
    private UUID featId;

    /** Where the feat came from: {@code manual}, {@code background}, {@code level_up}, {@code race}. */
    @Column(nullable = false, length = 24)
    @Builder.Default
    private String source = "manual";

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;
}
