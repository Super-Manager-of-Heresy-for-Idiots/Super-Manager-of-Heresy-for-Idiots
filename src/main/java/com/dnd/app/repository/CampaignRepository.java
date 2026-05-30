package com.dnd.app.repository;

import com.dnd.app.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findByInviteCode(String inviteCode);

    List<Campaign> findByIdIn(List<UUID> ids);
}
