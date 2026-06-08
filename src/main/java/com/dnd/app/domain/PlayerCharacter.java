package com.dnd.app.domain;

import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.ScoreMethod;
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

    @Column(name = "temp_hp", nullable = false)
    @Builder.Default
    private Integer tempHp = 0;

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

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "proficiencies", columnDefinition = "text")
    private String proficiencies;

    @Column(name = "equipment", columnDefinition = "text")
    private String equipment;

    @Column(length = 40)
    private String alignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id")
    private Background background;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "armor_class")
    private Integer armorClass;

    @Column(name = "speed")
    private Integer speed;

    @Column(name = "inspiration")
    @Builder.Default
    private Boolean inspiration = false;

    @Column(name = "hit_dice_type", length = 10)
    private String hitDiceType;

    @Column(name = "hit_dice_total", length = 20)
    private String hitDiceTotal;

    @Column(name = "death_save_successes")
    @Builder.Default
    private Integer deathSaveSuccesses = 0;

    @Column(name = "death_save_failures")
    @Builder.Default
    private Integer deathSaveFailures = 0;

    @Column(name = "saving_throw_proficiency_stat_ids_json", columnDefinition = "text")
    private String savingThrowProficiencyStatIdsJson;

    @Column(name = "biography_json", columnDefinition = "text")
    private String biographyJson;

    @Column(name = "features", columnDefinition = "text")
    private String features;

    @Column(name = "attacks_json", columnDefinition = "text")
    private String attacksJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_method", length = 20)
    private ScoreMethod scoreMethod;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterSkillProficiency> skillProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CharacterKnownSpell> knownSpells = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
