package com.dnd.app.domain;

import com.dnd.app.domain.content.FeatCategory;
import com.dnd.app.domain.content.FeatPrerequisite;
import com.dnd.app.domain.content.FeatSection;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс Feat описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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
    @JoinColumn(name = "category_id")
    private FeatCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean repeatable = false;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    // Авторство (P1-4): кто создал/изменил homebrew-контент; null для ванильных строк.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "feat", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FeatPrerequisite> prerequisites = new ArrayList<>();

    @OneToMany(mappedBy = "feat", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FeatSection> sections = new ArrayList<>();
}
