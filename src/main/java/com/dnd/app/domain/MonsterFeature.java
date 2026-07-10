package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс MonsterFeature описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "monster_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonsterFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    @Column(nullable = false, length = 30)
    private String section;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(name = "recharge_min")
    private Short rechargeMin;

    @Column(name = "recharge_max")
    private Short rechargeMax;

    @Column(name = "description_engloc", columnDefinition = "text")
    private String descriptionEngloc;

    @Column(name = "description_rusloc", columnDefinition = "text", nullable = false)
    private String descriptionRusloc;

    @Column(name = "attack_type", length = 10)
    private String attackType;

    @Column(name = "attack_bonus")
    private Short attackBonus;

    @Column(name = "reach_ft")
    private Short reachFt;

    @Column(name = "range_ft")
    private Short rangeFt;

    @Column(name = "range_long_ft")
    private Short rangeLongFt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "save_ability_id")
    private BestiaryAbility saveAbility;

    @Column(name = "save_dc")
    private Short saveDc;

    @OneToMany(mappedBy = "feature", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FeatureDamage> damages = new ArrayList<>();
}
