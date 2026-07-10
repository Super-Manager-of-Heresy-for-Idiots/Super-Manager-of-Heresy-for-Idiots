package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureChoiceOption описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_choice_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureChoiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "choice_group_id", nullable = false)
    private UUID choiceGroupId;

    /** {@link ChoiceOptionType} code. */
    @Column(name = "option_type", nullable = false, length = 24)
    private String optionType;

    /** Specific entity id for this option; null when the option set is defined by a filter. */
    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "filter_rule_id")
    private UUID filterRuleId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
