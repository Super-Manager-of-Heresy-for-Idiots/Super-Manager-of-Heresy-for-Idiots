package com.dnd.app.domain;

import com.dnd.app.domain.enums.DamageType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "feature_damages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureDamage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private MonsterFeature feature;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private Short average;

    @Column(length = 30)
    private String dice;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", length = 20)
    private DamageType damageType;

    @Column(columnDefinition = "text")
    private String note;
}
