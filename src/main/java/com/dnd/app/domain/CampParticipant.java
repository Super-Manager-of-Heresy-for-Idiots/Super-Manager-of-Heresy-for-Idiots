package com.dnd.app.domain;

import com.dnd.app.domain.enums.CampParticipantState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CampParticipant описывает участника привала: состояние отдыха, слот дозора,
 * назначенную даунтайм-активность и снимок результата отдыха. Отдых применяется
 * per-character, поэтому исход и ошибка транзакции хранятся здесь, а не на привале.
 */
@Entity
@Table(name = "camp_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camp_session_id", nullable = false)
    private CampSession campSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private CampParticipantState state = CampParticipantState.NOT_RESTED;

    /** Номер слота дозора в пределах привала; null — персонаж не в дозоре. */
    @Column(name = "watch_slot")
    private Integer watchSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private CampActivity activity;

    @Column(name = "activity_note", length = 300)
    private String activityNote;

    /** Снимок {@code RestResult} последнего применённого отдыха (JSON). */
    @Column(name = "rest_result_json", columnDefinition = "text")
    private String restResultJson;

    @Column(name = "rest_error_code", length = 60)
    private String restErrorCode;

    @Column(name = "rest_error_message", length = 500)
    private String restErrorMessage;

    @Column(name = "rested_at")
    private Instant restedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
