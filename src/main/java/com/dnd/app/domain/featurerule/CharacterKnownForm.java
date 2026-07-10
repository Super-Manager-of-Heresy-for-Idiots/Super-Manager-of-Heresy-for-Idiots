package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterKnownForm описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_known_form")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterKnownForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    /** Bestiary monster id (validated in the app layer). */
    @Column(name = "monster_id", nullable = false)
    private UUID monsterId;

    @Column(name = "source_feature_id")
    private UUID sourceFeatureId;

    @Column(name = "learned_at_level")
    private Integer learnedAtLevel;

    @Column(name = "approved_by_dm", nullable = false)
    @Builder.Default
    private boolean approvedByDm = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
