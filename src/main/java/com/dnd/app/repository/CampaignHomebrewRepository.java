package com.dnd.app.repository;

import com.dnd.app.domain.CampaignHomebrew;
import com.dnd.app.domain.CampaignHomebrewId;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт CampaignHomebrewRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignHomebrewRepository extends JpaRepository<CampaignHomebrew, CampaignHomebrewId> {

    List<CampaignHomebrew> findByCampaignId(UUID campaignId);

    boolean existsByCampaignIdAndPackageId(UUID campaignId, UUID packageId);

    void deleteByCampaignIdAndPackageId(UUID campaignId, UUID packageId);

    @Query("SELECT ch.packageId FROM CampaignHomebrew ch WHERE ch.campaignId = :campaignId")
    Set<UUID> findPackageIdsByCampaignId(@Param("campaignId") UUID campaignId);
}
