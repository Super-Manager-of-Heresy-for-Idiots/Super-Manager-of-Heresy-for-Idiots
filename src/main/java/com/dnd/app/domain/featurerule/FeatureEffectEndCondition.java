package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureEffectEndCondition описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_effect_end_condition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureEffectEndCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "effect_definition_id", nullable = false)
    private UUID effectDefinitionId;

    @Column(name = "trigger_event_type_id")
    private UUID triggerEventTypeId;

    @Column(name = "condition_id")
    private UUID conditionId;

    @Column(name = "same_feature_reuse", nullable = false)
    @Builder.Default
    private boolean sameFeatureReuse = false;

    @Column(name = "rest_type_id")
    private UUID restTypeId;

    @Column(name = "predicate_formula_id")
    private UUID predicateFormulaId;
}
