package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "blueprint_homebrew")
@IdClass(BlueprintHomebrewId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintHomebrew {

    @Id
    @Column(name = "blueprint_id")
    private UUID blueprintId;

    @Id
    @Column(name = "package_id")
    private UUID packageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", insertable = false, updatable = false)
    private CampaignBlueprint blueprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", insertable = false, updatable = false)
    private HomebrewPackage homebrewPackage;

    @Column(name = "pinned_version")
    private Integer pinnedVersion;
}
