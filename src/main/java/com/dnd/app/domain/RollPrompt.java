package com.dnd.app.domain;

import com.dnd.app.domain.enums.RollPromptStatus;
import com.dnd.app.domain.enums.RollPromptType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс RollPrompt описывает запрошенную мастером проверку (ROLL_PROMPT): мастер выбирает
 * персонажа, тип проверки и Сложность; у владельца персонажа появляется окно броска.
 * Бросок исполняется на сервере (d20 + модификатор), результат хранится здесь же.
 */
@Entity
@Table(name = "roll_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RollPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_type", nullable = false, length = 20)
    private RollPromptType rollType;

    /** Характеристика проверки/спасброска; null для CUSTOM. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stat_type_id")
    private StatType statType;

    /** Сложность (DC); null — просто бросок без порога успеха. */
    @Column(name = "dc")
    private Integer dc;

    /** Скрывать ли DC от игрока до броска. */
    @Column(name = "hide_dc", nullable = false)
    @Builder.Default
    private Boolean hideDc = false;

    /** NORMAL | ADVANTAGE | DISADVANTAGE. */
    @Column(name = "advantage_mode", nullable = false, length = 12)
    @Builder.Default
    private String advantageMode = "NORMAL";

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private RollPromptStatus status = RollPromptStatus.PENDING;

    // --- Результат броска (заполняется сервером при /roll) ---

    /** Выпавшее значение d20 (при преимуществе/помехе — учтённое). */
    @Column(name = "roll_natural")
    private Integer rollNatural;

    /** Второй d20 при преимуществе/помехе (для прозрачности), иначе null. */
    @Column(name = "roll_second")
    private Integer rollSecond;

    @Column(name = "modifier")
    private Integer modifier;

    @Column(name = "total")
    private Integer total;

    /** total >= dc; null, если DC не задан. */
    @Column(name = "success")
    private Boolean success;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "rolled_at")
    private Instant rolledAt;
}
