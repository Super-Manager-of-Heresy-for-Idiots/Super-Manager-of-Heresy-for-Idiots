package com.dnd.app.repository;

import com.dnd.app.domain.content.ContentCharacterClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ContentCharacterClassRepository extends JpaRepository<ContentCharacterClass, UUID> {

    Optional<ContentCharacterClass> findBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    List<ContentCharacterClass> findAllByHomebrewIsNull();

    List<ContentCharacterClass> findAllByHomebrewIdIn(Set<UUID> homebrewIds);
}
