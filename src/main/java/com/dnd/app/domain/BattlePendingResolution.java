package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BattlePendingResolution хранит «отложенный исход» боевого заклинания у конкретной цели (SAVE_PROMPT).
 * При касте заклинания со спасброском движок не решает за игрока: он катит кости урона, вычисляет рекомендацию
 * (спас vs DC) и создаёт эту строку. Ответственный за цель (игрок за своего персонажа, GM за остальных)
 * выбирает исход (весь урон / половина / не получать) и накладывать ли состояние — движок только рекомендует.
 * Одна строка = один отложенный исход одной цели от одного каста; при разрешении строка удаляется.
 */
@Entity
@Table(name = "battle_pending_resolution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattlePendingResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Цель эффекта — комбатант активного боя (FK battle_combatants, ON DELETE CASCADE). */
    @Column(name = "combatant_id", nullable = false)
    private UUID combatantId;

    /** Бой, к которому относится исход (FK battles, ON DELETE CASCADE). */
    @Column(name = "battle_id", nullable = false)
    private UUID battleId;

    /** Кастующий персонаж (для лога/WS); null для не-персонажа. */
    @Column(name = "caster_character_id")
    private UUID casterCharacterId;

    /** Название заклинания (для окна и лога). */
    @Column(name = "spell_name", length = 200)
    private String spellName;

    /** Скатанный урон до митигации и спаса (весь урон при провале). */
    @Column(name = "damage_amount", nullable = false)
    private int damageAmount;

    /** Тип урона (для митигации сопротивлений/иммунитета); null — нетипизированный. */
    @Column(name = "damage_type_id")
    private UUID damageTypeId;

    /** Урон половинится при успешном спасе (true) либо полностью снимается (false). */
    @Column(name = "half_on_save", nullable = false)
    @Builder.Default
    private boolean halfOnSave = false;

    /** Сложность спасброска (DC); null — спаса нет (тогда исход только «весь/ничего»). */
    @Column(name = "save_dc")
    private Integer saveDc;

    /** Слаг характеристики спасброска (dex/con/…). */
    @Column(name = "save_ability", length = 16)
    private String saveAbility;

    /** Рекомендованный исход движка: SUCCESS | FAIL | null. */
    @Column(name = "recommended_outcome", length = 16)
    private String recommendedOutcome;

    /** Рекомендованный бросок d20 (для показа «14 vs DC 15»). */
    @Column(name = "recommended_roll")
    private Integer recommendedRoll;

    /** Бонус спасброска цели (для показа итога d20+бонус). */
    @Column(name = "recommended_save_bonus")
    private Integer recommendedSaveBonus;

    /** CSV id накладываемых состояний (Фаза 2 — гейтинг состояний). */
    @Column(name = "condition_ids", columnDefinition = "text")
    private String conditionIds;

    /** Длительность состояний в раундах (Фаза 2). */
    @Column(name = "condition_duration_rounds")
    private Integer conditionDurationRounds;

    /** CSV id уже созданных active-effect (Фаза 2 — record-and-retract при успешном спасе). */
    @Column(name = "applied_active_effect_ids", columnDefinition = "text")
    private String appliedActiveEffectIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
