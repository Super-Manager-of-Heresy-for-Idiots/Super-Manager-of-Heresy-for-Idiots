package com.dnd.app.domain;

import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One append-only combat-log entry for a battle (Phase 1.2). Ordering + afterSeq pagination ride
 * {@code seq} (monotonic within a battle). References are raw UUIDs, not {@code @ManyToOne} — the log
 * never navigates them and must survive combatant deletion (the DB FKs are {@code ON DELETE SET NULL}
 * for actor/target). {@code payload} is JSON text (the core maps JSON as text) carrying the roll
 * formula / dice / modifier so the UI can expand a result.
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
