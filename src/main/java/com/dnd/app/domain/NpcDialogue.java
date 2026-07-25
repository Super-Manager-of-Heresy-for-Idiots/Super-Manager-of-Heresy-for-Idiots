package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс NpcDialogue описывает опциональный диалог NPC (WORLD_PLAN Этап 4): дерево реплик,
 * которое мастер задаёт по желанию. Один диалог на NPC. Само дерево хранится как JSON-текст
 * в {@code treeJson} (массив узлов), корневой узел — {@code rootNodeId}.
 */
@Entity
@Table(name = "npc_dialogues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpcDialogue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "npc_id", nullable = false, unique = true)
    private CampaignNpc npc;

    @Column(name = "root_node_id", length = 64)
    private String rootNodeId;

    @Column(name = "tree_json", columnDefinition = "text")
    private String treeJson;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
