package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creature_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatureType implements DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text", nullable = false)
    private String nameRusloc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @Column(name = "is_unique", nullable = false)
    @Builder.Default
    private Boolean isUnique = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
