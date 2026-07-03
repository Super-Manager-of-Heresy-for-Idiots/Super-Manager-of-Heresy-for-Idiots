package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Reference row for an action-economy type (action, bonus_action, reaction, …). Seeded in migration 066. */
@Entity
@Table(name = "action_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
