package com.dnd.app.domain.content;

import com.dnd.app.domain.Feat;
import com.dnd.app.domain.StatType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatPrerequisite описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feat_prerequisite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feat_prerequisite_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_id", nullable = false)
    private Feat feat;

    @Column(name = "prerequisite_type", nullable = false, columnDefinition = "text")
    private String prerequisiteType;

    @Column(name = "level_required")
    private Integer levelRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_score_id")
    private StatType abilityScore;

    @Column(name = "minimum_score")
    private Integer minimumScore;

    @Column(name = "group_key", columnDefinition = "text")
    private String groupKey;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
