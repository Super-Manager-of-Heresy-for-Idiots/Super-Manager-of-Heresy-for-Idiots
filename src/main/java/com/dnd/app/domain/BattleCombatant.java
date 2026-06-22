package com.dnd.app.domain;

import com.dnd.app.domain.enums.CombatantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A single participant on a battle's initiative tracker. Each row is one instance:
 * three goblins are three rows (each with its own roll, HP and label), and every joined
 * character is its own row. {@code monster} XOR {@code character} is populated depending
 * on {@link #type}.
 */
@Entity
@Table(name = "battle_combatants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleCombatant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @Enumerated(EnumType.STRING)
    @Column(name = "combatant_type", nullable = false, length = 12)
    private CombatantType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id")
    private Monster monster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private PlayerCharacter character;

    /** Display label, e.g. "Гоблин #2" for monsters or the character name. */
    @Column(name = "display_name", nullable = false, length = 140)
    private String displayName;

    /** 1-based ordinal among duplicates of the same monster; null for characters. */
    @Column(name = "instance_index")
    private Integer instanceIndex;

    /** Final initiative = roll + modifier. Null while still ASSEMBLING (not yet rolled). */
    @Column(name = "initiative")
    private Integer initiative;

    /** The raw d20 (1–20) used for this combatant's initiative. */
    @Column(name = "initiative_roll")
    private Integer initiativeRoll;

    /** Dexterity score captured for deterministic initiative tie-breaking. */
    @Column(name = "dex_tiebreak")
    private Integer dexTiebreak;

    /** 0-based position in the initiative order; assigned/recomputed on the tracker. */
    @Column(name = "turn_order")
    private Integer turnOrder;

    @Column(name = "current_hp")
    private Integer currentHp;

    @Column(name = "max_hp")
    private Integer maxHp;

    /** Action economy for the current turn; reset when this combatant's turn begins. */
    @Column(name = "action_used", nullable = false)
    @Builder.Default
    private Boolean actionUsed = false;

    @Column(name = "bonus_action_used", nullable = false)
    @Builder.Default
    private Boolean bonusActionUsed = false;

    @Column(name = "reaction_used", nullable = false)
    @Builder.Default
    private Boolean reactionUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
