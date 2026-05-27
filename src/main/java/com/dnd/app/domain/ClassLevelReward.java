package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_level_rewards",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_id", "required_level", "reward_type", "reward_id"},
                name = "uq_class_level_reward"))
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

    @Column(name = "reward_id", nullable = false)
    private UUID rewardId;

    @Column(name = "is_choice", nullable = false)
    @Builder.Default
    private Boolean isChoice = true;
}
