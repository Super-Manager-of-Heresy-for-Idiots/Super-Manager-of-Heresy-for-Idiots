package com.dnd.app.domain;

import com.dnd.app.domain.enums.BlueprintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_blueprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignBlueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CampaignBlueprint parent;

    @Column(name = "origin_version")
    private Integer originVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universe_id", nullable = false)
    private Universe universe;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "lore_description", columnDefinition = "text")
    private String loreDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BlueprintStatus status = BlueprintStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "allow_forks", nullable = false)
    @Builder.Default
    private Boolean allowForks = true;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "cover_url", columnDefinition = "text")
    private String coverUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
