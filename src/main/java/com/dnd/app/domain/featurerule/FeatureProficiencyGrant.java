package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A proficiency/expertise a feature rule confers (skill, weapon, armor, tool, saving throw). */
@Entity
@Table(name = "feature_proficiency_grant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureProficiencyGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** {@link FeatureProficiencyType} code. */
    @Column(name = "proficiency_type", nullable = false, length = 24)
    private String proficiencyType;

    /** Specific target entity id (skill/tool/…); null when the grant is a choice governed by a filter. */
    @Column(name = "target_id")
    private UUID targetId;

    /** Optional filter reference (validated in-app), when the grant is a choice over a pool. */
    @Column(name = "filter_rule_id")
    private UUID filterRuleId;

    @Column(nullable = false)
    @Builder.Default
    private boolean expertise = false;

    /** {@link GrantTiming} code. */
    @Column(name = "grant_timing", nullable = false, length = 24)
    @Builder.Default
    private String grantTiming = GrantTiming.LEVEL_UP.getCode();
}
