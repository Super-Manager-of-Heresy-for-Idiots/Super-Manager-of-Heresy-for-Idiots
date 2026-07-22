package com.dnd.app.domain;

import com.dnd.app.domain.enums.CharacterQuestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterQuest описывает запись журнала квестов персонажа (WORLD_PLAN Этап 2):
 * какой квест взят, у какого NPC, когда, и в каком он персональном статусе.
 * Уникальность (character_id, quest_id) — один квест берётся персонажем один раз;
 * повторное взятие после отказа реактивирует ту же строку.
 */
@Entity
@Table(name = "character_quests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "quest_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private CampaignQuest quest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CharacterQuestStatus status = CharacterQuestStatus.ACCEPTED;

    /** NPC, выдавший квест; null, если запись создана ГМом напрямую или NPC удалён. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "given_by_npc_id")
    private CampaignNpc givenByNpc;

    @CreationTimestamp
    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
