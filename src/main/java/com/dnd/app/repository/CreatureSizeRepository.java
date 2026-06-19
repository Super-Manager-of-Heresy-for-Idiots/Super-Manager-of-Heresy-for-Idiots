package com.dnd.app.repository;

import com.dnd.app.domain.CreatureSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreatureSizeRepository extends JpaRepository<CreatureSize, UUID> {

    List<CreatureSize> findByHomebrewIsNullOrderByNameRuAsc();

    Optional<CreatureSize> findBySlugAndHomebrewIsNull(String slug);

    Optional<CreatureSize> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
