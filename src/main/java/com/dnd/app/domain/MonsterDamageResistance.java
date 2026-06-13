package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "monster_damage_resistances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterDamageResistance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private BestiaryDamageType damageType;

    @Column(columnDefinition = "text")
    private String note;
}
