package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.NpcSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Класс BlueprintNpc описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "blueprint_npcs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintNpc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private CampaignBlueprint blueprint;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_visible_to_players", nullable = false)
    @Builder.Default
    private Boolean isVisibleToPlayers = false;

    @Column(name = "public_description", columnDefinition = "text")
    private String publicDescription;

    @Column(name = "private_description", columnDefinition = "text")
    private String privateDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private NpcSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private com.dnd.app.domain.content.Species race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ContentCharacterClass characterClass;

    @Column(name = "level")
    private Integer level;

    @Column(name = "abilities", columnDefinition = "text")
    private String abilities;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "blueprint_npc_spells",
            joinColumns = @JoinColumn(name = "npc_id"),
            inverseJoinColumns = @JoinColumn(name = "spell_id")
    )
    @Builder.Default
    private Set<Spell> spells = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_monster_id")
    private Monster sourceMonster;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
