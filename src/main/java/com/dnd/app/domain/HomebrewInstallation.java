package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "homebrew_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomebrewInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private HomebrewPackage homebrewPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id", nullable = false)
    private User installer;

    @Column(name = "installed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant installedAt = Instant.now();

    @Column(name = "source_version", nullable = false)
    private Integer sourceVersion;
}
