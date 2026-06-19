package com.dnd.app.repository;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.enums.BattleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleRepository extends JpaRepository<Battle, UUID> {

    List<Battle> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    Optional<Battle> findByIdAndCampaignId(UUID id, UUID campaignId);

    boolean existsByCampaignIdAndStatus(UUID campaignId, BattleStatus status);
}
