package com.dnd.app.domain;

import com.dnd.app.domain.enums.CombatantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BattleCombatant описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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

    // ---- Action economy for the current turn ---------------------------------------------
    // Actions and bonus actions are pools (a max count and how many have been spent this turn);
    // the spent counters reset when this combatant's turn begins. Every combatant starts with one
    // action and one bonus action; the maxima can grow with level, spells or GM adjustment.
    // Legendary actions default to none. Reactions stay a single per-round flag.

    @Column(name = "action_max", nullable = false)
    @Builder.Default
    private Integer actionMax = 1;

    @Column(name = "action_spent", nullable = false)
    @Builder.Default
    private Integer actionSpent = 0;

    @Column(name = "bonus_action_max", nullable = false)
    @Builder.Default
    private Integer bonusActionMax = 1;

    @Column(name = "bonus_action_spent", nullable = false)
    @Builder.Default
    private Integer bonusActionSpent = 0;

    @Column(name = "legendary_action_max", nullable = false)
    @Builder.Default
    private Integer legendaryActionMax = 0;

    @Column(name = "legendary_action_spent", nullable = false)
    @Builder.Default
    private Integer legendaryActionSpent = 0;

    @Column(name = "reaction_used", nullable = false)
    @Builder.Default
    private Boolean reactionUsed = false;

    // ---- Movement budget for the current turn --------------------------------------------
    // Feet already moved on this turn. The spatial cost of a path is computed in map-service; this
    // authoritative budget is validated against the combatant's speed by the internal movement
    // endpoint and reset (like the action economy above) when this combatant's turn begins.

    @Column(name = "movement_used_ft", nullable = false)
    @Builder.Default
    private Integer movementUsedFt = 0;

    // ---- Pending concentration save (Phase 2.2) ------------------------------------------
    // Set when a concentrating character takes damage: the DC of the Con save they must make to keep
    // concentration. The player/GM rolls it themselves (manual or AUTO) via the concentration-check
    // endpoint, which resolves and clears this. Null = no pending check.
    @Column(name = "pending_concentration_dc")
    private Integer pendingConcentrationDc;

    // ---- Standard-action turn state (Phase 2.7) ------------------------------------------
    // Set by the standard-action endpoint and (except `hidden`) cleared at the start of this
    // combatant's next turn — i.e. they last "until the start of your next turn" (D&D 5e).
    //   dashing       — movement budget is doubled this turn (Dash).
    //   dodging        — attacks against this combatant have disadvantage; advantage on Dex saves.
    //   disengaged     — this combatant's movement provokes no opportunity attacks this turn.
    //   hidden         — hidden from enemies; grants advantage on its next attack, then revealed.
    //   helpAdvantage  — an ally Helped this combatant: its next attack has advantage (consumed on use).

    @Column(name = "dashing", nullable = false)
    @Builder.Default
    private Boolean dashing = false;

    @Column(name = "dodging", nullable = false)
    @Builder.Default
    private Boolean dodging = false;

    @Column(name = "disengaged", nullable = false)
    @Builder.Default
    private Boolean disengaged = false;

    @Column(name = "hidden", nullable = false)
    @Builder.Default
    private Boolean hidden = false;

    @Column(name = "help_advantage", nullable = false)
    @Builder.Default
    private Boolean helpAdvantage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
