package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "character_stats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_char_stat", columnNames = {"character_id", "stat_type_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_type_id", nullable = false)
    private StatType statType;

    @Column(nullable = false)
    @Builder.Default
    private Integer value = 10;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;
}
