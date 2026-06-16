package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "feat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feat_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
