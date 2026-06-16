package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "spell")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "spell_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @Column(nullable = false)
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private SpellSchool school;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
