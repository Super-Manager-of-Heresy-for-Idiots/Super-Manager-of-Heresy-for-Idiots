package com.dnd.app.repository;

import com.dnd.app.domain.DamageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DamageTypeRepository extends JpaRepository<DamageType, UUID> {

    List<DamageType> findByHomebrewIsNullOrderByNameRuAsc();

    Optional<DamageType> findBySlugAndHomebrewIsNull(String slug);

    Optional<DamageType> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
