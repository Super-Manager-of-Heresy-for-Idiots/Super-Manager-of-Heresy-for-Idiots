package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс DamageType описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
    /**
     * Возвращает результат операции "get name rusloc" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getNameRusloc() {
        return nameRu;
    }

    /**
     * Устанавливает результат операции "set name rusloc" в рамках бизнес-логики домена.
     * @param nameRusloc входящее значение name rusloc, используемое бизнес-сценарием
     */
    @Override
    public void setNameRusloc(String nameRusloc) {
        this.nameRu = nameRusloc;
    }

    /**
     * Возвращает результат операции "get name engloc" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getNameEngloc() {
        return nameEn;
    }

    /**
     * Устанавливает результат операции "set name engloc" в рамках бизнес-логики домена.
     * @param nameEngloc входящее значение name engloc, используемое бизнес-сценарием
     */
    @Override
    public void setNameEngloc(String nameEngloc) {
        this.nameEn = nameEngloc;
    }
}
