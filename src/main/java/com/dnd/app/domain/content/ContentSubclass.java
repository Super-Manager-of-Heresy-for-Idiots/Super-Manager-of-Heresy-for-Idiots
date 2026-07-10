package com.dnd.app.domain.content;

import com.dnd.app.domain.HomebrewPackage;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс ContentSubclass описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "subclass")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSubclass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subclass_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ContentCharacterClass characterClass;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @Column(name = "is_empty_placeholder", nullable = false)
    @Builder.Default
    private Boolean emptyPlaceholder = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
