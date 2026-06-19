package com.dnd.app.repository;

import com.dnd.app.domain.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
}
