package com.dnd.app.domain.content;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.StatType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Класс ContentCharacterClass описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_class")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentCharacterClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_id")
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

    @Column(columnDefinition = "text")
    private String subtitle;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @Column(name = "hit_die")
    private Integer hitDie;

    @Column(name = "is_spellcaster", nullable = false)
    @Builder.Default
    private Boolean spellcaster = false;

    @Column(name = "has_cantrips", nullable = false)
    @Builder.Default
    private Boolean hasCantrips = false;

    @Column(name = "is_half_caster", nullable = false)
    @Builder.Default
    private Boolean halfCaster = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spellcasting_ability_id")
    private StatType spellcastingAbility;

    /** Caster progression: FULL | HALF | THIRD | PACT | NONE. Null => derive from spellcaster/halfCaster. */
    @Column(name = "caster_type", length = 16)
    private String casterType;

    /** Spell preparation model: PREPARED | KNOWN. Null for non-casters / unknown. */
    @Column(name = "preparation", length = 16)
    private String preparation;

    /** True when the class records spells in a spellbook (Wizard). */
    @Column(name = "uses_spellbook", nullable = false)
    @Builder.Default
    private Boolean usesSpellbook = false;

    /** True when the class can ritual-cast. */
    @Column(name = "ritual_casting", nullable = false)
    @Builder.Default
    private Boolean ritualCasting = false;

    @Column(name = "skill_choice_count", nullable = false)
    @Builder.Default
    private Integer skillChoiceCount = 0;

    @Column(name = "skill_choice_any", nullable = false)
    @Builder.Default
    private Boolean skillChoiceAny = false;

    @Column(name = "armor_proficiency_text", columnDefinition = "text")
    private String armorProficiencyText;

    @Column(name = "weapon_proficiency_text", columnDefinition = "text")
    private String weaponProficiencyText;

    @Column(name = "tool_proficiency_text", columnDefinition = "text")
    private String toolProficiencyText;

    @ManyToMany
    @JoinTable(
            name = "class_saving_throw",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "ability_score_id")
    )
    @Builder.Default
    private Set<StatType> savingThrows = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "class_primary_ability",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "ability_score_id")
    )
    @Builder.Default
    private Set<StatType> primaryAbilities = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "class_skill_option",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<ContentSkill> skillOptions = new HashSet<>();
}
