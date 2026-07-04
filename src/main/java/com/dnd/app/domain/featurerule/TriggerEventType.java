package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Reference row for a gameplay event type (feature_used, attack_resolved, …). Seeded in migration 066. */
@Entity
@Table(name = "trigger_event_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriggerEventType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 48, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
