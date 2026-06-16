package com.dnd.app.domain.content;

import com.dnd.app.domain.StatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "character_reward_ability_score_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterRewardAbilityScoreSelection {

    @EmbeddedId
    private CharacterRewardAbilityScoreSelectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("characterRewardSelectionId")
    @JoinColumn(name = "character_reward_selection_id")
    private CharacterRewardSelection selection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rewardGrantId")
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrantAbilityScore grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("abilityScoreId")
    @JoinColumn(name = "ability_score_id")
    private StatType abilityScore;

    @Column(name = "bonus_amount", nullable = false)
    private Integer bonusAmount;
}
