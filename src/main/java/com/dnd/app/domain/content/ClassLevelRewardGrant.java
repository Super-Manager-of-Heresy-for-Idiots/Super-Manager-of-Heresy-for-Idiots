package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс ClassLevelRewardGrant описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "class_level_reward_grant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reward_grant_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_group_id")
    private ClassLevelRewardGroup rewardGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_option_id")
    private ClassLevelRewardOption rewardOption;

    @Column(name = "grant_type", nullable = false, columnDefinition = "text")
    private String grantType;

    @Column(name = "label_ru", columnDefinition = "text")
    private String labelRu;

    @Column(name = "label_en", columnDefinition = "text")
    private String labelEn;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
