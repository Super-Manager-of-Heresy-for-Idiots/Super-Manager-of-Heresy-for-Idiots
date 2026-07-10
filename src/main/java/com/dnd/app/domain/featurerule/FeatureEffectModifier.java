package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureEffectModifier описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_effect_modifier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureEffectModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "effect_definition_id", nullable = false)
    private UUID effectDefinitionId;

    @Column(name = "modifier_type", nullable = false, length = 32)
    private String modifierType;

    @Column(name = "target_selector_id")
    private UUID targetSelectorId;

    @Column(name = "value_formula_id")
    private UUID valueFormulaId;

    @Column(name = "damage_type_id")
    private UUID damageTypeId;

    @Column(name = "ability_id")
    private UUID abilityId;

    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "condition_id")
    private UUID conditionId;
}
