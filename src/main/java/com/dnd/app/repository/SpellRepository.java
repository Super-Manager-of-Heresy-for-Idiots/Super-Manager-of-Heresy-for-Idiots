package com.dnd.app.repository;

import com.dnd.app.domain.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт SpellRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SpellRepository extends JpaRepository<Spell, UUID> {

    boolean existsByNameRu(String nameRu);

    List<Spell> findAllByHomebrewIsNull();

    List<Spell> findAllByHomebrewIdIn(Set<UUID> packageIds);

    @Query("""
            SELECT s FROM Spell s
            WHERE (s.homebrew IS NULL OR s.homebrew.id IN :packageIds)
            AND (:level IS NULL OR s.level = :level)
            AND (:school IS NULL OR s.school.slug = :school)
            """)
    List<Spell> findFiltered(@Param("packageIds") Set<UUID> packageIds,
                             @Param("level") Integer level,
                             @Param("school") String school);

    @Query("""
            SELECT s FROM Spell s
            WHERE s.homebrew IS NULL
            AND (:level IS NULL OR s.level = :level)
            AND (:school IS NULL OR s.school.slug = :school)
            """)
    List<Spell> findFilteredSystemOnly(@Param("level") Integer level,
                                       @Param("school") String school);

    List<Spell> findByIdIn(Set<UUID> ids);

    /** Spells flagged for manual resolution review, school eagerly fetched for the admin list. */
    @Query("""
            SELECT s FROM Spell s
            LEFT JOIN FETCH s.school
            WHERE s.warning = true
            ORDER BY s.level ASC, s.nameRu ASC
            """)
    List<Spell> findWarnings();
}
