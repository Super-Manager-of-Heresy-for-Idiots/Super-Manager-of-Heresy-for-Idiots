package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.NpcSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс CampaignNpc описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "campaign_npcs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignNpc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

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

    // How the NPC was authored. NULL => legacy free-form NPC (pre-047).
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private NpcSourceType sourceType;

    // The NPC's world role (merchant, quest giver, …). NULL => unspecified.
    @Enumerated(EnumType.STRING)
    @Column(name = "npc_role", length = 20)
    private NpcRole npcRole;

    // --- CLASS_BASED build (all optional except race/class/level, enforced in service) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private com.dnd.app.domain.content.Species race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ContentCharacterClass characterClass;

    @Column(name = "level")
    private Integer level;

    // Free-form, progression-independent abilities/features described by the GM.
    @Column(name = "abilities", columnDefinition = "text")
    private String abilities;

    // Free, progression-independent spell selection. Not validated against level/class.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "campaign_npc_spells",
            joinColumns = @JoinColumn(name = "npc_id"),
            inverseJoinColumns = @JoinColumn(name = "spell_id")
    )
    @Builder.Default
    private Set<Spell> spells = new LinkedHashSet<>();

    // --- MONSTER_BASED build ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_monster_id")
    private Monster sourceMonster;

    @OneToMany(mappedBy = "npc", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NpcNote> notes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
