package com.dnd.app.domain;

import com.dnd.app.domain.enums.RaceSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_races")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterRace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @Column(length = 80)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_engloc", columnDefinition = "text")
    private String descriptionEngloc;

    @Column(name = "description_rusloc", columnDefinition = "text")
    private String descriptionRusloc;

    @Column(name = "lore_description", columnDefinition = "text")
    private String loreDescription;

    @Column(name = "lore_description_engloc", columnDefinition = "text")
    private String loreDescriptionEngloc;

    @Column(name = "lore_description_rusloc", columnDefinition = "text")
    private String loreDescriptionRusloc;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    @Builder.Default
    private RaceSourceType sourceType = RaceSourceType.SYSTEM;

    @Column(name = "source_name", length = 120)
    private String sourceName;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @Column(name = "creature_type", nullable = false, length = 40)
    @Builder.Default
    private String creatureType = "HUMANOID";

    @Column(name = "size_options_json", columnDefinition = "text")
    private String sizeOptionsJson;

    @Column(name = "default_size", length = 20)
    private String defaultSize;

    @Column(name = "speed_json", columnDefinition = "text")
    private String speedJson;

    @Column(name = "darkvision_range")
    private Integer darkvisionRange;

    @Column(name = "traits_json", columnDefinition = "text")
    private String traitsJson;

    @Column(name = "lineages_json", columnDefinition = "text")
    private String lineagesJson;

    @Column(name = "lineage_required", nullable = false)
    @Builder.Default
    private Boolean lineageRequired = false;

    @Column(name = "languages_json", columnDefinition = "text")
    private String languagesJson;

    @Column(name = "language_options_json", columnDefinition = "text")
    private String languageOptionsJson;

    @Column(name = "proficiencies_json", columnDefinition = "text")
    private String proficienciesJson;

    @Column(name = "resistances_json", columnDefinition = "text")
    private String resistancesJson;

    @Column(name = "vulnerabilities_json", columnDefinition = "text")
    private String vulnerabilitiesJson;

    @Column(name = "immunities_json", columnDefinition = "text")
    private String immunitiesJson;

    @Column(name = "condition_resistances_json", columnDefinition = "text")
    private String conditionResistancesJson;

    @Column(name = "condition_advantages_json", columnDefinition = "text")
    private String conditionAdvantagesJson;

    @Column(name = "innate_spells_json", columnDefinition = "text")
    private String innateSpellsJson;

    @Column(name = "allow_ability_score_bonuses", nullable = false)
    @Builder.Default
    private Boolean allowAbilityScoreBonuses = false;

    @Column(name = "ability_score_bonuses_json", columnDefinition = "text")
    private String abilityScoreBonusesJson;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
