package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "condition_modifiers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cond_stat", columnNames = {"condition_id", "stat_type_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_type_id", nullable = false)
    private StatType statType;

    @Column(name = "modifier_value", nullable = false)
    private Integer modifierValue;
}
