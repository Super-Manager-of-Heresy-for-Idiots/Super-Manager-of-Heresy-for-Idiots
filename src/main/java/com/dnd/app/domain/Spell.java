package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.domain.content.SpellComponent;
import com.dnd.app.domain.content.SpellDamage;
import com.dnd.app.domain.content.SpellHealing;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "spell")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "spell_id")
    private UUID id;

    @Column(name = "mod_id")
    private UUID modId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @Column(nullable = false)
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private SpellSchool school;

    @Column(name = "casting_time_raw", columnDefinition = "text")
    private String castingTimeRaw;

    @Column(name = "casting_action_slug", columnDefinition = "text")
    private String castingActionSlug;

    @Column(name = "is_ritual", nullable = false)
    @Builder.Default
    private Boolean ritual = false;

    @Column(name = "range_type", columnDefinition = "text")
    private String rangeType;

    @Column(name = "range_distance")
    private Integer rangeDistance;

    @Column(name = "range_unit", columnDefinition = "text")
    private String rangeUnit;

    @Column(name = "duration_raw", columnDefinition = "text")
    private String durationRaw;

    @Column(name = "duration_type", columnDefinition = "text")
    private String durationType;

    @Column(name = "duration_amount")
    private Integer durationAmount;

    @Column(name = "duration_unit", columnDefinition = "text")
    private String durationUnit;

    @Column(nullable = false)
    @Builder.Default
    private Boolean concentration = false;

    @Column(name = "save_ability", columnDefinition = "text")
    private String saveAbility;

    @Column(name = "is_attack_roll", nullable = false)
    @Builder.Default
    private Boolean attackRoll = false;

    @Column(name = "check_ability", columnDefinition = "text")
    private String checkAbility;

    @Column(name = "check_skill", columnDefinition = "text")
    private String checkSkill;

    @Column(name = "is_warning", nullable = false)
    @Builder.Default
    private Boolean warning = false;

    @Column(name = "warning_reason", columnDefinition = "text")
    private String warningReason;

    // ---- Structured area-of-effect / lingering zone (Phase 2.3, changeset 091) ------------
    // Populated by SpellAreaBackfillService parsing the RU description; admin-editable afterwards.

    /** SPHERE / CUBE / CONE / CYLINDER / LINE; null = the spell has no area of effect. */
    @Column(name = "area_shape", columnDefinition = "text")
    private String areaShape;

    /** The shape's size in feet: sphere/cylinder radius, cube edge, cone/line length. */
    @Column(name = "area_size_ft")
    private Integer areaSizeFt;

    /** DIFFICULT when the lingering zone is difficult terrain (Web); null = none. */
    @Column(name = "zone_terrain", columnDefinition = "text")
    private String zoneTerrain;

    /** LIGHT / HEAVY when the zone obscures its area (Web = LIGHT); null = none. */
    @Column(name = "zone_obscurement", columnDefinition = "text")
    private String zoneObscurement;

    /** True when the spell leaves a zone for its duration (Web); false = instant burst (Fireball). */
    @Column(name = "zone_persists", nullable = false)
    @Builder.Default
    private Boolean zonePersists = false;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "higher_levels", columnDefinition = "text")
    private String higherLevels;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "spell_component", joinColumns = @JoinColumn(name = "spell_id"))
    @Builder.Default
    private List<SpellComponent> components = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "spell_damage", joinColumns = @JoinColumn(name = "spell_id"))
    @Builder.Default
    private List<SpellDamage> damages = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "spell_healing", joinColumns = @JoinColumn(name = "spell_id"))
    @Builder.Default
    private List<SpellHealing> healings = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "spell_class",
            joinColumns = @JoinColumn(name = "spell_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @Builder.Default
    private Set<ContentCharacterClass> classes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "spell_subclass",
            joinColumns = @JoinColumn(name = "spell_id"),
            inverseJoinColumns = @JoinColumn(name = "subclass_id")
    )
    @Builder.Default
    private Set<ContentSubclass> subclasses = new HashSet<>();

    /**
     * Bestiary statblocks this summon/conjuration spell summons or whose stats it
     * uses (e.g. Find Familiar's beast forms, Phantom Steed -> Riding Horse). Empty
     * for non-summon spells. Read-only through the domain model; the join rows and
     * their provenance columns are populated by the 059 migration, not by JPA.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "spell_summon_monster",
            joinColumns = @JoinColumn(name = "spell_id"),
            inverseJoinColumns = @JoinColumn(name = "monster_id")
    )
    @Builder.Default
    private Set<Monster> summonedMonsters = new HashSet<>();

    /**
     * Buffs/debuffs this spell applies when cast. Authored in the admin spell editor and stored in
     * the {@code spell_buffs} join table. Owning side: adding/removing from this set writes the
     * join rows (no cascade to the {@link BuffDebuff} entities themselves).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "spell_buffs",
            joinColumns = @JoinColumn(name = "spell_id"),
            inverseJoinColumns = @JoinColumn(name = "buff_debuff_id")
    )
    @Builder.Default
    private Set<BuffDebuff> linkedBuffs = new HashSet<>();
}
