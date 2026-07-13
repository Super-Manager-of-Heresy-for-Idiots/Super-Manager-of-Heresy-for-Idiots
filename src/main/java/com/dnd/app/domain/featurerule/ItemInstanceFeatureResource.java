package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Текущее состояние ресурса feature-rules, привязанного к конкретному экземпляру предмета.
 */
@Entity
@Table(name = "item_instance_feature_resource",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_instance_feature_resource_def",
                columnNames = {"item_instance_id", "resource_definition_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemInstanceFeatureResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "item_instance_id", nullable = false)
    private UUID itemInstanceId;

    @Column(name = "resource_definition_id", nullable = false)
    private UUID resourceDefinitionId;

    @Column(name = "current_value", nullable = false)
    private Integer currentValue;

    @Column(name = "max_value_snapshot", nullable = false)
    private Integer maxValueSnapshot;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
