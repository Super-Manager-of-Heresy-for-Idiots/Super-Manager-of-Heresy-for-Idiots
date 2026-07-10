package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterFeatureChoice описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_feature_choice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterFeatureChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    /** The owning class feature ({@code class_feature.class_feature_id}). */
    @Column(name = "feature_id", nullable = false)
    private UUID featureId;

    @Column(name = "choice_group_id", nullable = false)
    private UUID choiceGroupId;

    /** {@link ChoiceOptionType} code. */
    @Column(name = "option_type", nullable = false, length = 24)
    private String optionType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "chosen_at_level")
    private Integer chosenAtLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
