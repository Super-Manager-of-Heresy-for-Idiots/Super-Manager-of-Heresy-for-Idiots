package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterFeat описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_feats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterFeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "feat_id", nullable = false)
    private UUID featId;

    /** Where the feat came from: {@code manual}, {@code background}, {@code level_up}, {@code race}. */
    @Column(nullable = false, length = 24)
    @Builder.Default
    private String source = "manual";

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;
}
