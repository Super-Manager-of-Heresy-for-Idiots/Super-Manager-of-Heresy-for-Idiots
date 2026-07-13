package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс FeatureActiveEffect описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_active_effect")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureActiveEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "source_character_id")
    private UUID sourceCharacterId;

    @Column(name = "source_feature_id")
    private UUID sourceFeatureId;

    @Column(name = "source_item_instance_id")
    private UUID sourceItemInstanceId;

    @Column(name = "effect_definition_id", nullable = false)
    private UUID effectDefinitionId;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "remaining_rounds")
    private Integer remainingRounds;

    @Column(name = "state_json", columnDefinition = "text")
    private String stateJson;

    /** {@link ActiveEffectStatus} code. */
    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = ActiveEffectStatus.ACTIVE.getCode();
}
