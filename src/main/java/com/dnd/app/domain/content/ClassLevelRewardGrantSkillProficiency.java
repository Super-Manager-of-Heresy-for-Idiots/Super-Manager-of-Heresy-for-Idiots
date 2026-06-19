package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_skill_proficiency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantSkillProficiency {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private ContentSkill skill;

    @Column(name = "choose_count", nullable = false)
    @Builder.Default
    private Integer chooseCount = 1;

    @Column(name = "any_skill", nullable = false)
    @Builder.Default
    private Boolean anySkill = false;

    @Column(name = "raw_filter_text", columnDefinition = "text")
    private String rawFilterText;

    @ManyToMany
    @JoinTable(
            name = "class_level_reward_grant_skill_option",
            joinColumns = @JoinColumn(name = "reward_grant_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<ContentSkill> skillOptions = new HashSet<>();
}
