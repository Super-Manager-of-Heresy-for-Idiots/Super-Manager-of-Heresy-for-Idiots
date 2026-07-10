package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс CharacterHitDie описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
