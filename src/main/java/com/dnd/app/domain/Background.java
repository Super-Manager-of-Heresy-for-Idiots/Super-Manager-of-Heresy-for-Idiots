package com.dnd.app.domain;

import com.dnd.app.domain.content.BackgroundEquipmentChoiceGroup;
import com.dnd.app.domain.content.BackgroundFeatOption;
import com.dnd.app.domain.content.BackgroundLanguageProficiency;
import com.dnd.app.domain.content.BackgroundToolProficiency;
import com.dnd.app.domain.content.ContentSkill;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс Background описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "background")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Background {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_id")
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
    private String description;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_id")
    private Feat grantedFeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "background_ability_option",
            joinColumns = @JoinColumn(name = "background_id"),
            inverseJoinColumns = @JoinColumn(name = "ability_score_id"))
    @Builder.Default
    private List<StatType> abilityOptions = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "background_skill_proficiency",
            joinColumns = @JoinColumn(name = "background_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @Builder.Default
    private List<ContentSkill> skillProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "background", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BackgroundFeatOption> featOptions = new ArrayList<>();

    @OneToMany(mappedBy = "background", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BackgroundToolProficiency> toolProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "background", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BackgroundLanguageProficiency> languageProficiencies = new ArrayList<>();

    @OneToMany(mappedBy = "background", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BackgroundEquipmentChoiceGroup> equipmentChoiceGroups = new ArrayList<>();
}
