package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassFeatureRepository extends JpaRepository<ClassFeature, UUID> {

    Optional<ClassFeature> findByCharacterClassIdAndSubclassIsNullAndSlug(UUID classId, String slug);

    Optional<ClassFeature> findBySubclassIdAndSlug(UUID subclassId, String slug);

    List<ClassFeature> findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(UUID classId);

    List<ClassFeature> findAllBySubclassIdOrderByLevelAscSortOrderAsc(UUID subclassId);

    /** Class features whose parsed mechanics need admin review. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT f FROM ClassFeature f
            LEFT JOIN FETCH f.characterClass
            LEFT JOIN FETCH f.subclass
            WHERE f.warning = true
            ORDER BY f.level ASC, f.title ASC
            """)
    List<ClassFeature> findWarnings();
}
