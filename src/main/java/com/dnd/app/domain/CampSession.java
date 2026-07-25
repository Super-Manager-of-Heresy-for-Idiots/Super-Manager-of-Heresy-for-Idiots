package com.dnd.app.domain;

import com.dnd.app.domain.enums.CampInterruptReason;
import com.dnd.app.domain.enums.CampStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс CampSession описывает привал отряда — агрегат лагеря со своей state-machine
 * (SETTING_UP → ACTIVE → RESTING → COMPLETED, ветка INTERRUPTED). Сам по себе привал
 * механику не считает: он оркестрирует уже существующие сервисы отдыха, склада и боя.
 * Инвариант "один незавершённый привал на кампанию" держится частичным уникальным индексом.
 */
@Entity
@Table(name = "camp_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** Локация привала; null — отряд встал в пути, вне справочника локаций. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private CampaignLocation location;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Игровой день кампании: свободная нумерация мастера. */
    @Column(name = "day_number")
    private Integer dayNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private CampStatus status = CampStatus.SETTING_UP;

    /** Канонический код запущенного отдыха ({@code long_rest} / {@code short_rest}); null до старта. */
    @Column(name = "rest_type", length = 20)
    private String restType;

    /** Решение мастера при прерывании: засчитать ли отряду частичный отдых. */
    @Column(name = "apply_partial_rest", nullable = false)
    @Builder.Default
    private Boolean applyPartialRest = false;

    @Column(name = "watch_slot_count", nullable = false)
    @Builder.Default
    private Integer watchSlotCount = 0;

    /** Расписание дозора: JSON-массив {@code [{"slot":1,"label":"20:00 — 22:30"}, …]}. */
    @Column(name = "watch_schedule_json", columnDefinition = "text")
    private String watchScheduleJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "interrupt_reason", length = 20)
    private CampInterruptReason interruptReason;

    /** Событие журнала, прервавшее привал (обычно засада). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interrupt_event_id")
    private CampEvent interruptEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "campSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampParticipant> participants = new ArrayList<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "rest_started_at")
    private Instant restStartedAt;

    @Column(name = "rest_completed_at")
    private Instant restCompletedAt;

    @Column(name = "interrupted_at")
    private Instant interruptedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
