package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureActionCost описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_action_cost")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureActionCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    @Column(name = "action_type_id", nullable = false)
    private UUID actionTypeId;

    @Column(nullable = false)
    @Builder.Default
    private Integer amount = 1;

    /** Optional predicate gating whether the cost applies in the current context. */
    @Column(name = "condition_formula_id")
    private UUID conditionFormulaId;
}
