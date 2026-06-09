package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "spells")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120, unique = true)
    private String name;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, length = 30)
    private String school;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_engloc", columnDefinition = "text")
    private String descriptionEngloc;

    @Column(name = "description_rusloc", columnDefinition = "text")
    private String descriptionRusloc;

    @Column(name = "available_to_class_ids_json", columnDefinition = "text")
    private String availableToClassIdsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
