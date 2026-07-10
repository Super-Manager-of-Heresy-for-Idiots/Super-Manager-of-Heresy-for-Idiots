package com.dnd.app.repository;

import com.dnd.app.domain.CampaignLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CampaignLocationRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignLocationRepository extends JpaRepository<CampaignLocation, UUID> {

    List<CampaignLocation> findByCampaignId(UUID campaignId);

    List<CampaignLocation> findByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);
}
