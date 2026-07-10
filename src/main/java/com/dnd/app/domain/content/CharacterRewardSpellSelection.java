package com.dnd.app.domain.content;

import com.dnd.app.domain.Spell;
import jakarta.persistence.*;
import lombok.*;

/**
 * Класс CharacterRewardSpellSelection описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_reward_spell_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterRewardSpellSelection {

    @EmbeddedId
    private CharacterRewardSpellSelectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("characterRewardSelectionId")
    @JoinColumn(name = "character_reward_selection_id")
    private CharacterRewardSelection selection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rewardGrantId")
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrantSpell grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("spellId")
    @JoinColumn(name = "spell_id")
    private Spell spell;
}
