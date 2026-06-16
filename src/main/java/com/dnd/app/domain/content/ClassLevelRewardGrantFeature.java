package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_feature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantFeature {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_feature_id", nullable = false)
    private ClassFeature classFeature;
}
