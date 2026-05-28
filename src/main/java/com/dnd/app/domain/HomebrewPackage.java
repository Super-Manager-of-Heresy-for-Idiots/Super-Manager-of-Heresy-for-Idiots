package com.dnd.app.domain;

import com.dnd.app.domain.enums.HomebrewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "homebrew_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomebrewPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HomebrewStatus status = HomebrewStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

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

    @ManyToMany
    @JoinTable(
            name = "homebrew_package_tags",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<HomebrewTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "homebrewPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomebrewContentItem> contentItems = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
