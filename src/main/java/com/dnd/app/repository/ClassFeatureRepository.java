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
}
