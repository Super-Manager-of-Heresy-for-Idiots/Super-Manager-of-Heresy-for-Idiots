package com.dnd.app.repository;

import com.dnd.app.domain.CampaignQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CampaignQuestRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignQuestRepository extends JpaRepository<CampaignQuest, UUID> {

    List<CampaignQuest> findByCampaignId(UUID campaignId);

    List<CampaignQuest> findByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);
}
