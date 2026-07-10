package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс GameplayEvent описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "gameplay_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameplayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** {@code trigger_event_type} code (feature_used, attack_resolved, …). */
    @Column(name = "event_type", nullable = false, length = 48)
    private String eventType;

    @Column(name = "combat_id")
    private UUID combatId;

    @Column(name = "actor_character_id")
    private UUID actorCharacterId;

    /** JSON payload (target ids, amounts, context). */
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
