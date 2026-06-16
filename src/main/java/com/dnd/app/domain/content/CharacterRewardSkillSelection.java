package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "character_reward_skill_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterRewardSkillSelection {

    @EmbeddedId
    private CharacterRewardSkillSelectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("characterRewardSelectionId")
    @JoinColumn(name = "character_reward_selection_id")
    private CharacterRewardSelection selection;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rewardGrantId")
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrantSkillProficiency grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private ContentSkill skill;
}
