package com.dnd.app.repository;

import com.dnd.app.domain.Monster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт MonsterRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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

    // Removes a campaign's own monsters (NOT system/homebrew); monster_* children and
    // battle_combatants drop via DB ON DELETE CASCADE. Used when deleting the campaign.
    @Modifying
    @Query("delete from Monster m where m.campaign.id = :campaignId")
    void deleteByCampaignId(@Param("campaignId") UUID campaignId);
}
