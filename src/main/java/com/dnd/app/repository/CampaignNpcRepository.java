package com.dnd.app.repository;

import com.dnd.app.domain.CampaignNpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignNpcRepository extends JpaRepository<CampaignNpc, UUID> {

    List<CampaignNpc> findByCampaignId(UUID campaignId);

    List<CampaignNpc> findByCampaignIdAndIsVisibleToPlayersTrue(UUID campaignId);
}
