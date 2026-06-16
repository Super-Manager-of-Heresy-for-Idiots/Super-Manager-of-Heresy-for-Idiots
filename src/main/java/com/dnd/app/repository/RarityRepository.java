package com.dnd.app.repository;

import com.dnd.app.domain.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RarityRepository extends JpaRepository<Rarity, UUID> {

    Optional<Rarity> findBySlugAndHomebrewIsNull(String slug);

    Optional<Rarity> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
