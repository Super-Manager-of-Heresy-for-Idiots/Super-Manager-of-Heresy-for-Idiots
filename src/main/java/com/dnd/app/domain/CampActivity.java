package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CampActivity описывает даунтайм-активность привала (готовка, ковка, разведка…).
 * Системные активности не принадлежат кампании ({@code campaign == null}) и доступны всем;
 * кастомные создаёт мастер конкретной кампании. Механических автоэффектов у активности нет —
 * награды выдаёт мастер обычными инструментами.
 */
@Entity
@Table(name = "camp_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** null — системная активность, доступная всем кампаниям. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Код глифа для интерфейса (flame, sword, eye, scroll, …). */
    @Column(name = "icon_code", length = 40)
    private String iconCode;

    /** Подсказка мастеру, какую проверку уместно запросить (ROLL_PROMPT). */
    @Column(name = "skill_check_ref", length = 60)
    private String skillCheckRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
