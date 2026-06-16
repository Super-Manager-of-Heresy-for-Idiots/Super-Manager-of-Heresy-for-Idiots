package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "spell_school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpellSchool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "spell_school_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;
}
