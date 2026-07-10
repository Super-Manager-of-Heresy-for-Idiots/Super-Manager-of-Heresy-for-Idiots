package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Класс CharacterRewardAbilityScoreSelectionId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CharacterRewardAbilityScoreSelectionId implements Serializable {

    @Column(name = "character_reward_selection_id")
    private UUID characterRewardSelectionId;

    @Column(name = "reward_grant_id")
    private UUID rewardGrantId;

    @Column(name = "ability_score_id")
    private UUID abilityScoreId;
}
