package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterFeatureCompanion описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_feature_companion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterFeatureCompanion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "source_feature_id")
    private UUID sourceFeatureId;

    /** Bestiary monster id for the stat block (validated in the app layer). */
    @Column(name = "monster_id")
    private UUID monsterId;

    @Column(name = "custom_name", length = 120)
    private String customName;

    @Column(name = "hp_formula_id")
    private UUID hpFormulaId;

    @Column(name = "ac_formula_id")
    private UUID acFormulaId;

    @Column(name = "attack_bonus_formula_id")
    private UUID attackBonusFormulaId;

    /** active | dismissed | dead. */
    @Column(nullable = false, length = 16)
    @Builder.Default
    private String state = "active";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
