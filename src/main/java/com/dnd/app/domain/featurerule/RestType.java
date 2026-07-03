package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Reference row for a rest/reset window (short_rest, long_rest, dawn, …). Seeded in migration 066. */
@Entity
@Table(name = "rest_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestType {

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
