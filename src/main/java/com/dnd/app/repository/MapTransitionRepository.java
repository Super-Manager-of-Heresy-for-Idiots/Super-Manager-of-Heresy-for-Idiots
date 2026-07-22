package com.dnd.app.repository;

import com.dnd.app.domain.MapTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт MapTransitionRepository описывает репозиторий переходов между картами
 * (WORLD_PLAN Этап 5).
 */
public interface MapTransitionRepository extends JpaRepository<MapTransition, UUID> {

    List<MapTransition> findByCampaignId(UUID campaignId);

    List<MapTransition> findByCampaignIdAndFromMapId(UUID campaignId, UUID fromMapId);
}
