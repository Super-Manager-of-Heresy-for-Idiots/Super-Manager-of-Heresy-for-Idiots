package com.dnd.app.domain;

import com.dnd.app.domain.enums.ObjectiveType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс QuestObjective описывает опциональную цель квеста (WORLD_PLAN Этап 3): что нужно
 * сделать персонажу для выполнения квеста. Мастер добавляет цели по желанию; квест без целей
 * сдаётся без ограничений. {@code targetRef} — необязательная ссылка на связанную сущность
 * (item_template / бестиарий / npc / локация) в зависимости от {@link ObjectiveType}.
 */
@Entity
@Table(name = "quest_objectives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private CampaignQuest quest;

    @Enumerated(EnumType.STRING)
    @Column(name = "objective_type", nullable = false, length = 20)
    private ObjectiveType objectiveType;

    @Column(name = "target_ref")
    private UUID targetRef;

    @Column(name = "target_label", length = 200)
    private String targetLabel;

    @Column(name = "required_count", nullable = false)
    @Builder.Default
    private Integer requiredCount = 1;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}
