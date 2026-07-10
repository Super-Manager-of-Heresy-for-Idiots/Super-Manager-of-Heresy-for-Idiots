package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureSpellGrant описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_spell_grant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSpellGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** Specific spell; null when the grant is defined by a filter. */
    @Column(name = "spell_id")
    private UUID spellId;

    @Column(name = "spell_filter_id")
    private UUID spellFilterId;

    @Column(name = "counts_against_known", nullable = false)
    @Builder.Default
    private boolean countsAgainstKnown = false;

    @Column(name = "always_prepared", nullable = false)
    @Builder.Default
    private boolean alwaysPrepared = false;

    @Column(name = "cast_without_slot", nullable = false)
    @Builder.Default
    private boolean castWithoutSlot = false;

    @Column(name = "uses_resource_definition_id")
    private UUID usesResourceDefinitionId;

    @Column(name = "spellcasting_ability_override_id")
    private UUID spellcastingAbilityOverrideId;
}
