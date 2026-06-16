package com.dnd.app.repository;

import com.dnd.app.domain.content.ContentSubclass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ContentSubclassRepository extends JpaRepository<ContentSubclass, UUID> {

    Optional<ContentSubclass> findByCharacterClassIdAndSlugAndHomebrewIsNull(UUID classId, String slug);

    boolean existsByCharacterClassIdAndSlugAndHomebrewIsNull(UUID classId, String slug);

    List<ContentSubclass> findAllByCharacterClassId(UUID classId);

    List<ContentSubclass> findAllByHomebrewIsNull();

    List<ContentSubclass> findAllByHomebrewIdIn(Set<UUID> homebrewIds);
}
