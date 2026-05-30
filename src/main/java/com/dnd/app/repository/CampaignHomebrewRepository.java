package com.dnd.app.repository;

import com.dnd.app.domain.CampaignHomebrew;
import com.dnd.app.domain.CampaignHomebrewId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignHomebrewRepository extends JpaRepository<CampaignHomebrew, CampaignHomebrewId> {

    List<CampaignHomebrew> findByCampaignId(UUID campaignId);

    boolean existsByCampaignIdAndPackageId(UUID campaignId, UUID packageId);

    void deleteByCampaignIdAndPackageId(UUID campaignId, UUID packageId);
}
