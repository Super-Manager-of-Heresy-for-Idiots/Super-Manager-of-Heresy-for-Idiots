package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A pool of hit dice of one size (d{@code die}) a character has. {@code total} = sum of class levels of
 * classes using that die; {@code remaining} = unspent (spent on short rests, half regained on a long rest).
 * One row per (character, die).
 */
@Entity
@Table(name = "character_hit_dice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterHitDie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    /** Die size: 6, 8, 10, 12. */
    @Column(nullable = false)
    private Integer die;

    @Column(nullable = false)
    @Builder.Default
    private Integer total = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer remaining = 0;
}
