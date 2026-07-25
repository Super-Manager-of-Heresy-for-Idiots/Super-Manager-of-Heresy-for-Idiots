package com.dnd.app.domain;

import com.dnd.app.domain.enums.CampEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CampEvent описывает запись журнала привала: засаду, встречу, сюжетный момент,
 * погоду или произвольное событие мастера. Заготовка события может быть скрыта от игроков
 * до триггера; засада дополнительно связывается с созданным боем.
 */
@Entity
@Table(name = "camp_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camp_session_id", nullable = false)
    private CampSession campSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CampEventType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** Внутриигровое время события: свободная метка мастера («01:20»). */
    @Column(name = "occurred_label", length = 20)
    private String occurredLabel;

    @Column(name = "visible_to_players", nullable = false)
    @Builder.Default
    private Boolean visibleToPlayers = true;

    /** Бой, созданный из засады, если мастер выбрал создание энкаунтера. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    /** Момент срабатывания события; null — заготовка мастера, ещё не сработавшая. */
    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
