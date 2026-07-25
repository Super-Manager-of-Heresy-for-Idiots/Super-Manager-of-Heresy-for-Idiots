package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс MapTransition описывает переход между картами через "ключевые клетки"
 * (WORLD_PLAN Этап 5): набор клеток на исходной карте, целевая карта и точка появления,
 * опционально смена локации мира. Карты — внешние id map-service (слабые ссылки).
 */
@Entity
@Table(name = "map_transitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** Карта map-service, с которой ведёт переход. */
    @Column(name = "from_map_id", nullable = false)
    private UUID fromMapId;

    /** JSON-массив ключевых клеток [{"gridX":..,"gridY":..}, ...]. */
    @Column(name = "from_cells_json", nullable = false, columnDefinition = "text")
    private String fromCellsJson;

    /** Карта map-service, на которую ведёт переход. */
    @Column(name = "to_map_id", nullable = false)
    private UUID toMapId;

    /** JSON-точка появления {"gridX":..,"gridY":..}. */
    @Column(name = "to_cell_json", nullable = false, columnDefinition = "text")
    private String toCellJson;

    /** Локация мира, в которую попадает персонаж после перехода (или null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private CampaignLocation toLocation;

    /** Подпись перехода: "Дверь в подвал", "Портал" и т.п. */
    @Column(length = 120)
    private String label;

    /** ГМ может "запереть" переход, не удаляя его. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
