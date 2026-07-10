package com.dnd.app.domain;

import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BattleLog описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "battle_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "battle_id", nullable = false)
    private UUID battleId;

    /** Monotonic within a battle; drives ordering and afterSeq pagination. */
    @Column(name = "seq", nullable = false)
    private long seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private BattleLogType type;

    @Column(name = "actor_combatant_id")
    private UUID actorCombatantId;

    @Column(name = "target_combatant_id")
    private UUID targetCombatantId;

    /** JSON text (nullable) — the roll formula, dice, damage modifier, etc. */
    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 16)
    private BattleLogVisibility visibility;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
