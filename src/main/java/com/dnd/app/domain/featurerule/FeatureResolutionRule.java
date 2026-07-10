package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureResolutionRule описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_resolution_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureResolutionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** saving_throw | ability_check | skill_check | attack_roll | contested_check. */
    @Column(name = "resolution_type", nullable = false, length = 24)
    private String resolutionType;

    @Column(name = "ability_id")
    private UUID abilityId;

    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "dc_formula_id")
    private UUID dcFormulaId;

    @Column(name = "on_success_rule_id")
    private UUID onSuccessRuleId;

    @Column(name = "on_failure_rule_id")
    private UUID onFailureRuleId;
}
