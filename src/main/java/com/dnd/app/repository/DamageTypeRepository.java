package com.dnd.app.repository;

import com.dnd.app.domain.DamageType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Now extends {@link DictionaryRepository} (code-based lookups for the bestiary dictionary framework)
 * on top of the original slug-based lookups (rules / content side) — one repository for the unified
 * damage type.
 */
public interface DamageTypeRepository extends DictionaryRepository<DamageType> {

    List<DamageType> findByHomebrewIsNullOrderByNameRuAsc();

    Optional<DamageType> findBySlugAndHomebrewIsNull(String slug);

    Optional<DamageType> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
