package com.dnd.app.domain;

import com.dnd.app.domain.enums.CharacterStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "total_level", nullable = false)
    @Builder.Default
    private Integer totalLevel = 1;

    @Column(nullable = false)
    @Builder.Default
    private Long experience = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CharacterStatus status = CharacterStatus.ACTIVE;

    @Column(name = "current_hp")
    private Integer currentHp;

    @Column(name = "max_hp")
    private Integer maxHp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private CharacterRace race;

    @Column(name = "selected_lineage_id")
    private UUID selectedLineageId;

    @Column(name = "race_snapshot_json", columnDefinition = "text")
    private String raceSnapshotJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @OneToMany(mappedBy = "character", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<CharacterClassLevel> classLevels = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterStat> stats = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterActiveEffect> activeEffects = new ArrayList<>();

    @OneToMany(mappedBy = "ownerCharacter")
    @Builder.Default
    private List<ItemInstance> itemInstances = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
