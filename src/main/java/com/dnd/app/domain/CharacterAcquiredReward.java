package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_acquired_rewards",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"character_id", "class_level_reward_id"},
                name = "uq_char_reward"))
/**
 * Legacy character reward acquisition table for old class_level_rewards.
 * New selectable rewards must use character_reward_selection and child selection tables.
 */
@Deprecated(forRemoval = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterAcquiredReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_level_reward_id", nullable = false)
    private ClassLevelReward classLevelReward;

    @CreationTimestamp
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private Instant acquiredAt;
}
