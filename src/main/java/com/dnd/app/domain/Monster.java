package com.dnd.app.domain;

import com.dnd.app.domain.enums.CreatureSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "monsters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_external_id", unique = true)
    private Long sourceExternalId;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text", nullable = false)
    private String nameRusloc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alignment_id")
    private Alignment alignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreatureSize size;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_secondary", length = 20)
    private CreatureSize sizeSecondary;

    @Column(name = "is_swarm", nullable = false)
    @Builder.Default
    private Boolean isSwarm = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "swarm_size", length = 20)
    private CreatureSize swarmSize;

    @Column(name = "armor_class", nullable = false)
    private Short armorClass;

    @Column(name = "armor_class_text", columnDefinition = "text")
    private String armorClassText;

    @Column(name = "initiative_bonus")
    private Short initiativeBonus;

    @Column(name = "initiative_score")
    private Short initiativeScore;

    @Column(name = "hp_average")
    private Integer hpAverage;

    @Column(name = "hp_dice_count")
    private Short hpDiceCount;

    @Column(name = "hp_dice_sides")
    private Short hpDiceSides;

    @Column(name = "hp_dice_modifier")
    private Integer hpDiceModifier;

    @Column(name = "hp_formula", columnDefinition = "text")
    private String hpFormula;

    @Column(name = "str_score", nullable = false)
    private Short strScore;

    @Column(name = "dex_score", nullable = false)
    private Short dexScore;

    @Column(name = "con_score", nullable = false)
    private Short conScore;

    @Column(name = "int_score", nullable = false)
    private Short intScore;

    @Column(name = "wis_score", nullable = false)
    private Short wisScore;

    @Column(name = "cha_score", nullable = false)
    private Short chaScore;

    @Column(name = "passive_perception")
    private Short passivePerception;

    @Column(name = "telepathy_ft")
    private Integer telepathyFt;

    @Column(name = "cr_rating", nullable = false, length = 10)
    private String crRating;

    @Column(name = "cr_value", nullable = false, precision = 5, scale = 3)
    private BigDecimal crValue;

    @Column(name = "xp_base")
    private Integer xpBase;

    @Column(name = "xp_lair")
    private Integer xpLair;

    @Column(name = "proficiency_bonus")
    private Short proficiencyBonus;

    @Column(name = "legendary_uses_base")
    private Short legendaryUsesBase;

    @Column(name = "legendary_uses_lair")
    private Short legendaryUsesLair;

    @Column(name = "legendary_text", columnDefinition = "text")
    private String legendaryText;

    @Column(name = "lore_text", columnDefinition = "text")
    private String loreText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @Column(name = "is_visible_to_players", nullable = false)
    @Builder.Default
    private Boolean isVisibleToPlayers = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_monster_id")
    private Monster sourceMonster;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
            name = "monster_creature_types",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "creature_type_id")
    )
    @Builder.Default
    private Set<CreatureType> creatureTypes = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monster_languages",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    @Builder.Default
    private Set<Language> languages = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monster_condition_immunities",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "condition_id")
    )
    @Builder.Default
    private Set<BestiaryCondition> conditionImmunities = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monster_habitats",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "habitat_id")
    )
    @Builder.Default
    private Set<Habitat> habitats = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monster_treasures",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "treasure_tag_id")
    )
    @Builder.Default
    private Set<TreasureTag> treasureTags = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monster_sources",
            joinColumns = @JoinColumn(name = "monster_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id")
    )
    @Builder.Default
    private Set<Source> sources = new HashSet<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterSpeed> speeds = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterSense> senses = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterSavingThrow> savingThrows = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterSkillProficiency> skillProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterDamageResistance> damageResistances = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterDamageImmunity> damageImmunities = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterDamageVulnerability> damageVulnerabilities = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterGear> gear = new ArrayList<>();

    @OneToMany(mappedBy = "monster", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MonsterFeature> features = new ArrayList<>();
}
