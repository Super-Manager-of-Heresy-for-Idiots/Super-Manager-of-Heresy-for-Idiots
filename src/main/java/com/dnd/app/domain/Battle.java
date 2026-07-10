package com.dnd.app.domain;

import com.dnd.app.domain.enums.BattleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс Battle описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "battles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BattleStatus status = BattleStatus.ASSEMBLING;

    /**
     * GM-overridden total combat XP. When {@code null}, the total is derived from the
     * sum of the monster group's base XP (see preview calculation).
     */
    @Column(name = "override_xp")
    private Integer overrideXp;

    /** 1-based combat round; incremented when the turn order wraps back to the top. */
    @Column(name = "round_number", nullable = false)
    @Builder.Default
    private Integer roundNumber = 1;

    /** Index into the initiative-ordered combatant list whose turn it currently is. */
    @Column(name = "current_turn_index", nullable = false)
    @Builder.Default
    private Integer currentTurnIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BattleCombatant> combatants = new ArrayList<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
