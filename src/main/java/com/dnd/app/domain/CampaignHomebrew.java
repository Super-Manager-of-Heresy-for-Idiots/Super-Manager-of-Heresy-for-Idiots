package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "campaign_homebrew")
@IdClass(CampaignHomebrewId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignHomebrew {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Id
    @Column(name = "package_id")
    private UUID packageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", insertable = false, updatable = false)
    private HomebrewPackage homebrewPackage;

    @Column(name = "pinned_version")
    private Integer pinnedVersion;
}
