package com.dnd.app.domain.content;

import com.dnd.app.domain.StatType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_ability_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantAbilityScore {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ability_score_id")
    private StatType abilityScore;

    @Column(name = "choose_count", nullable = false)
    @Builder.Default
    private Integer chooseCount = 1;

    @Column(name = "bonus_per_choice", nullable = false)
    @Builder.Default
    private Integer bonusPerChoice = 1;

    @Column(name = "total_bonus")
    private Integer totalBonus;

    @Column(name = "max_per_ability")
    private Integer maxPerAbility;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "raw_filter_text", columnDefinition = "text")
    private String rawFilterText;

    @ManyToMany
    @JoinTable(
            name = "class_level_reward_grant_ability_option",
            joinColumns = @JoinColumn(name = "reward_grant_id"),
            inverseJoinColumns = @JoinColumn(name = "ability_score_id")
    )
    @Builder.Default
    private Set<StatType> abilityOptions = new HashSet<>();
}
