package com.dnd.app.repository;

import com.dnd.app.domain.CampaignMember;
import com.dnd.app.domain.enums.CampaignRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    Optional<CampaignMember> findByCampaignIdAndUserId(UUID campaignId, UUID userId);

    List<CampaignMember> findByCampaignIdAndKickedFalse(UUID campaignId);

    List<CampaignMember> findByUserId(UUID userId);

    List<CampaignMember> findByUserIdAndKickedFalse(UUID userId);

    long countByCampaignIdAndRoleInCampaignAndKickedFalse(UUID campaignId, CampaignRole role);

    boolean existsByCampaignIdAndUserIdAndKickedFalse(UUID campaignId, UUID userId);
}
