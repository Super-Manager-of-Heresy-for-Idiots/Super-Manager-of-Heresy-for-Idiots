package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс ClassLevelRewardGroup описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "class_level_reward_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reward_group_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ContentCharacterClass characterClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_feature_id")
    private ClassFeature classFeature;

    @Column(name = "class_level", nullable = false)
    private Integer classLevel;

    @Column(name = "group_kind", nullable = false, columnDefinition = "text")
    private String groupKind;

    @Column(name = "prompt_ru", columnDefinition = "text")
    private String promptRu;

    @Column(name = "prompt_en", columnDefinition = "text")
    private String promptEn;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "choose_min", nullable = false)
    @Builder.Default
    private Integer chooseMin = 0;

    @Column(name = "choose_max", nullable = false)
    @Builder.Default
    private Integer chooseMax = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean repeatable = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "rewardGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ClassLevelRewardOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "rewardGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ClassLevelRewardGrant> grants = new ArrayList<>();
}
