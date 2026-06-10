package com.dnd.app.repository;

import com.dnd.app.domain.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MonsterRepository extends JpaRepository<Monster, UUID> {

    Optional<Monster> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // System monsters (both scopes null)
    List<Monster> findAllByCampaignIsNullAndHomebrewIsNull();

    List<Monster> findAllByCampaignIsNullAndHomebrewIsNullAndIsActiveTrue();

    // Homebrew-scoped monsters
    List<Monster> findAllByHomebrewId(UUID homebrewId);

    List<Monster> findAllByHomebrewIdIn(Set<UUID> homebrewIds);

    List<Monster> findAllByHomebrewIdAndIsActiveTrue(UUID homebrewId);

    // Campaign-scoped monsters
    List<Monster> findAllByCampaignId(UUID campaignId);

    List<Monster> findAllByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);
}
