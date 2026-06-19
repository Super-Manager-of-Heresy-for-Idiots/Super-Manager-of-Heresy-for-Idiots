package com.dnd.app.repository;

import com.dnd.app.domain.content.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpeciesRepository extends JpaRepository<Species, UUID> {

    Optional<Species> findBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    List<Species> findAllByHomebrewIsNull();

    List<Species> findAllByHomebrewIdIn(Set<UUID> homebrewIds);
}
