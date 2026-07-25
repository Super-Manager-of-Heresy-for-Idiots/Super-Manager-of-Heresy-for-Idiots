package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс LocationMap описывает привязку карты map-service к локации кампании
 * (WORLD_PLAN Этап 4). external_map_id — "слабая" ссылка на MapDefinition во внешнем
 * map-service (без FK): целостность проверяется на уровне сервиса при привязке.
 */
@Entity
@Table(name = "location_maps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"location_id", "external_map_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private CampaignLocation location;

    /** Идентификатор MapDefinition во внешнем map-service. */
    @Column(name = "external_map_id", nullable = false)
    private UUID externalMapId;

    /** Карта по умолчанию для боёв в этой локации. */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
