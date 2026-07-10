package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterTransformation описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_transformation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterTransformation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "source_feature_id")
    private UUID sourceFeatureId;

    @Column(name = "monster_id")
    private UUID monsterId;

    /** Links to the Stage 7 active effect that is this transformation's lifecycle container. */
    @Column(name = "active_effect_id")
    private UUID activeEffectId;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "transformation_policy_id")
    private UUID transformationPolicyId;

    @Column(name = "hp_mode", length = 24)
    private String hpMode;

    @Column(name = "retained_traits_policy", length = 24)
    private String retainedTraitsPolicy;

    /** active | ended. */
    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "active";

    @Column(name = "state_json", columnDefinition = "text")
    private String stateJson;
}
