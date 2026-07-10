package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BattleCombatantCondition описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "battle_combatant_condition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleCombatantCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combatant_id", nullable = false)
    private BattleCombatant combatant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private BestiaryCondition condition;

    @Column(name = "source_text")
    private String sourceText;

    /** Rounds left; null = until removed. Decremented on the round boundary; removed at 0. */
    @Column(name = "remaining_rounds")
    private Integer remainingRounds;

    @Column(name = "applied_round")
    private Integer appliedRound;

    @Column(name = "applied_by")
    private UUID appliedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
