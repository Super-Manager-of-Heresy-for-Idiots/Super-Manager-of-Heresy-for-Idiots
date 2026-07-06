package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * The single canonical damage type (fire, cold, poison, …). Merged from three former tables
 * (this {@code damage_type}, the orphan {@code damage_types}, and {@code bestiary_damage_types}) so
 * every subsystem — rules, spells, weapons, items, skills, species traits, monsters — shares one
 * reference. Exposes BOTH the rules-facing {@code slug} (lowercase) and the dictionary-facing
 * {@code code} (uppercase), kept mirrored, and implements {@link DictionaryEntry} so the bestiary
 * dictionary framework uses it directly.
 */
@Entity
@Table(name = "damage_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageType implements DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "damage_type_id")
    private UUID id;

    /** Lowercase english key (fire, cold, …); URL / rules facing. */
    @Column(columnDefinition = "text")
    private String slug;

    /** Uppercase english code (FIRE, COLD, …); dictionary facing. Kept in sync with {@link #slug}. */
    @Column(length = 60)
    private String code;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

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

    /** Keep slug/code mirrored so both the rules side (slug) and dictionary side (code) always resolve. */
    @PrePersist
    @PreUpdate
    void syncSlugCode() {
        if (slug == null && code != null) {
            slug = code.toLowerCase();
        } else if (code == null && slug != null) {
            code = slug.toUpperCase();
        }
    }

    // DictionaryEntry — the localized names map onto the existing name_ru / name_en columns.
    @Override
    public String getNameRusloc() {
        return nameRu;
    }

    @Override
    public void setNameRusloc(String nameRusloc) {
        this.nameRu = nameRusloc;
    }

    @Override
    public String getNameEngloc() {
        return nameEn;
    }

    @Override
    public void setNameEngloc(String nameEngloc) {
        this.nameEn = nameEngloc;
    }
}
