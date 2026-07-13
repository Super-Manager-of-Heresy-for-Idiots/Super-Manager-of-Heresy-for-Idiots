package com.dnd.app.domain.content;

import com.dnd.app.domain.CreatureSize;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс Species описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "species")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "species_id")
    private UUID id;

    @Column(name = "mod_id")
    private UUID modId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creature_type_id")
    private ContentCreatureType creatureType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @ManyToMany
    @JoinTable(
            name = "species_size_option",
            joinColumns = @JoinColumn(name = "species_id"),
            inverseJoinColumns = @JoinColumn(name = "character_size_id")
    )
    @Builder.Default
    private Set<CreatureSize> sizeOptions = new HashSet<>();

    @OneToMany(mappedBy = "species", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpeciesSpeed> speeds = new ArrayList<>();

    @OneToMany(mappedBy = "species", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<SpeciesTrait> traits = new ArrayList<>();

    // --- SP-1: авторинг видов ---
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Канонический payload авторинга (богатый FE-контракт RaceRequest) — лоссовый round-trip для RaceEditor. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "authoring_json", columnDefinition = "jsonb")
    private String authoringJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
