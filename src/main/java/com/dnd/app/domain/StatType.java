package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ability_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ability_score_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", nullable = false, columnDefinition = "text")
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
