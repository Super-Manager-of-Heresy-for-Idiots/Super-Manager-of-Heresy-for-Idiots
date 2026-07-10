package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Класс CharacterRewardSpellSelectionId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CharacterRewardSpellSelectionId implements Serializable {

    @Column(name = "character_reward_selection_id")
    private UUID characterRewardSelectionId;

    @Column(name = "reward_grant_id")
    private UUID rewardGrantId;

    @Column(name = "spell_id")
    private UUID spellId;
}
