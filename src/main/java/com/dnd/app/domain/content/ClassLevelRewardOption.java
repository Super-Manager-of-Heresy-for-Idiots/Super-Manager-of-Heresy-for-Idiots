package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс ClassLevelRewardOption описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "class_level_reward_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reward_option_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_group_id", nullable = false)
    private ClassLevelRewardGroup rewardGroup;

    @Column(name = "option_key", columnDefinition = "text")
    private String optionKey;

    @Column(name = "label_ru", nullable = false, columnDefinition = "text")
    private String labelRu;

    @Column(name = "label_en", columnDefinition = "text")
    private String labelEn;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_recommended", nullable = false)
    @Builder.Default
    private Boolean recommended = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "rewardOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ClassLevelRewardGrant> grants = new ArrayList<>();
}
