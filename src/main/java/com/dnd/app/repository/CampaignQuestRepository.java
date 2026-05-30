package com.dnd.app.repository;

import com.dnd.app.domain.CampaignQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignQuestRepository extends JpaRepository<CampaignQuest, UUID> {

    List<CampaignQuest> findByCampaignId(UUID campaignId);

    List<CampaignQuest> findByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);
}
