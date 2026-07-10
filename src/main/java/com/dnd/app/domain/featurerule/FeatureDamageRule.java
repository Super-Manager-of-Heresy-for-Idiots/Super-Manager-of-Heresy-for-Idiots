package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureDamageRule описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_damage_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureDamageRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "dice_formula_id")
    private UUID diceFormulaId;

    @Column(name = "flat_amount_formula_id")
    private UUID flatAmountFormulaId;

    @Column(name = "damage_type_id")
    private UUID damageTypeId;

    @Column(name = "target_type_id")
    private UUID targetTypeId;

    @Column(name = "requires_attack_hit", nullable = false)
    @Builder.Default
    private boolean requiresAttackHit = false;

    @Column(name = "requires_save", nullable = false)
    @Builder.Default
    private boolean requiresSave = false;

    @Column(name = "half_on_save", nullable = false)
    @Builder.Default
    private boolean halfOnSave = false;

    @Column(name = "save_rule_id")
    private UUID saveRuleId;
}
