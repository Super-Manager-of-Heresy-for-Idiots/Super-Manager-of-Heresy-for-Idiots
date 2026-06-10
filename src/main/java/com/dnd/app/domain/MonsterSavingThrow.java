package com.dnd.app.domain;

import com.dnd.app.domain.enums.Ability;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "monster_saving_throws")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterSavingThrow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ability ability;

    @Column(nullable = false)
    private Short bonus;
}
