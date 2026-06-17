package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "character_classes")
/**
 * Legacy class catalog mapped to the old plural content table.
 * New class content must use {@link com.dnd.app.domain.content.ContentCharacterClass}.
 */
@Deprecated(forRemoval = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_engloc", columnDefinition = "text")
    private String descriptionEngloc;

    @Column(name = "description_rusloc", columnDefinition = "text")
    private String descriptionRusloc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @Column(name = "hit_die")
    @Builder.Default
    private Integer hitDie = 8;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_ability_stat_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private StatType primaryAbilityStat;

    @Column(name = "saving_throw_stat_ids_json", columnDefinition = "text")
    private String savingThrowStatIdsJson;

    @Column(name = "skill_choice_count")
    @Builder.Default
    private Integer skillChoiceCount = 2;

    @Column(name = "skill_choice_option_ids_json", columnDefinition = "text")
    private String skillChoiceOptionIdsJson;

    @Column(name = "armor_weapon_proficiencies", columnDefinition = "text")
    private String armorWeaponProficiencies;

    @Column(name = "armor_weapon_proficiencies_engloc", columnDefinition = "text")
    private String armorWeaponProficienciesEngloc;

    @Column(name = "armor_weapon_proficiencies_rusloc", columnDefinition = "text")
    private String armorWeaponProficienciesRusloc;

    @Column(name = "is_spellcaster", nullable = false)
    @Builder.Default
    private Boolean isSpellcaster = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spellcasting_stat_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private StatType spellcastingStat;

    @Column(name = "has_cantrips", nullable = false)
    @Builder.Default
    private Boolean hasCantrips = false;

    @Column(name = "is_half_caster", nullable = false)
    @Builder.Default
    private Boolean isHalfCaster = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;
}
