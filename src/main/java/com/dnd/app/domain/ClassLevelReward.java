package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_level_rewards",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_id", "required_level", "reward_type", "reward_id"},
                name = "uq_class_level_reward"))
/**
 * Legacy flat reward model. New class rewards must use class_level_reward_group,
 * class_level_reward_option, class_level_reward_grant and typed grant tables.
 */
// Phase 12: archive-only — read by the legacy level-up flow until runtime data is migrated;
// new flows use class_level_reward_group/option/grant.
@Deprecated(forRemoval = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private CharacterClass characterClass;

    @Column(name = "required_level", nullable = false)
    private Integer requiredLevel;

    @Column(name = "reward_type", nullable = false, length = 30)
    private String rewardType;

    // nullable: параметрические награды (например ASI) не ссылаются на сущность.
    @Column(name = "reward_id")
    private UUID rewardId;

    @Column(name = "is_choice", nullable = false)
    @Builder.Default
    private Boolean isChoice = true;
}
