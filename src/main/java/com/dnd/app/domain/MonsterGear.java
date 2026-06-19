package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "monster_gear")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterGear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private MonsterGearItem item;

    @Column(nullable = false)
    @Builder.Default
    private Short qty = 1;
}
