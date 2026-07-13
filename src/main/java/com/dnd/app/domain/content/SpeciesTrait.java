package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс SpeciesTrait описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "species_trait")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeciesTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "species_trait_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "trait", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpeciesTraitEffect> effects = new ArrayList<>();
}
