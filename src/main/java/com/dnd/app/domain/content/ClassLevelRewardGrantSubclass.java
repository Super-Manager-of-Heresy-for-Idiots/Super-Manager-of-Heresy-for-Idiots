package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_subclass")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantSubclass {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id", nullable = false)
    private ContentSubclass subclass;
}
