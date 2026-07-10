package com.dnd.app.domain.content;

import com.dnd.app.domain.PlayerCharacter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterRewardSelection описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_reward_selection",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_character_reward_selection_option",
                columnNames = {"character_id", "reward_group_id", "reward_option_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterRewardSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "character_reward_selection_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_group_id", nullable = false)
    private ClassLevelRewardGroup rewardGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_option_id", nullable = false)
    private ClassLevelRewardOption rewardOption;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;

    @Column(name = "note_text", columnDefinition = "text")
    private String noteText;
}
