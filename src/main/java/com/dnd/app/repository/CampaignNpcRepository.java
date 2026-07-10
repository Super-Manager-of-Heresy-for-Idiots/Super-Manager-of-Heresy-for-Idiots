package com.dnd.app.repository;

import com.dnd.app.domain.CampaignNpc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CampaignNpcRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignNpcRepository extends JpaRepository<CampaignNpc, UUID> {

    List<CampaignNpc> findByCampaignId(UUID campaignId);

    List<CampaignNpc> findByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);

    // Detach NPCs from this campaign's monsters before those monsters are deleted
    // (campaign_npcs.source_monster_id has no ON DELETE action).
    @Modifying
    @Query("update CampaignNpc n set n.sourceMonster = null where n.campaign.id = :campaignId")
    void clearSourceMonsterByCampaignId(@Param("campaignId") UUID campaignId);
}
