package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterQuestObjectiveProgress описывает прогресс персонажа по конкретной цели квеста
 * (WORLD_PLAN Этап 3). Строка создаётся лениво, когда мастер выставляет прогресс по цели.
 * Для целей типа COLLECT_ITEM строка не нужна — прогресс вычисляется по инвентарю на лету.
 */
@Entity
@Table(name = "character_quest_objective_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"character_quest_id", "objective_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterQuestObjectiveProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_quest_id", nullable = false)
    private CharacterQuest characterQuest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objective_id", nullable = false)
    private QuestObjective objective;

    @Column(name = "current_count", nullable = false)
    @Builder.Default
    private Integer currentCount = 0;

    @Column(name = "completed_at")
    private Instant completedAt;
}
