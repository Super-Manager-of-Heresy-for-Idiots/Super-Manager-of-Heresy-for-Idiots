package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A live condition instance on a battle combatant (from the {@code bestiary_conditions} catalogue).
 * {@code remainingRounds} null means "until removed"; otherwise it decrements on each round boundary
 * and the row is deleted at 0. Phase 1.1 tracks the marker + duration; advantage/save automation from
 * conditions is Phase 2, layered on the same table.
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
