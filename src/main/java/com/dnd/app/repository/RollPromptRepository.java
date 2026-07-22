package com.dnd.app.repository;

import com.dnd.app.domain.RollPrompt;
import com.dnd.app.domain.enums.RollPromptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт RollPromptRepository описывает репозиторий запрошенных мастером проверок
 * (ROLL_PROMPT).
 */
public interface RollPromptRepository extends JpaRepository<RollPrompt, UUID> {

    @EntityGraph(attributePaths = {"character", "statType", "requestedBy"})
    List<RollPrompt> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    @EntityGraph(attributePaths = {"character", "statType", "requestedBy"})
    List<RollPrompt> findByCampaignIdAndStatusOrderByCreatedAtDesc(UUID campaignId, RollPromptStatus status);

    @EntityGraph(attributePaths = {"character", "statType", "requestedBy"})
    List<RollPrompt> findByCampaignIdAndCharacter_Owner_IdAndStatusOrderByCreatedAtAsc(
            UUID campaignId, UUID ownerId, RollPromptStatus status);

    @EntityGraph(attributePaths = {"character", "statType", "requestedBy"})
    List<RollPrompt> findByCampaignIdAndCharacter_Owner_IdOrderByCreatedAtDesc(UUID campaignId, UUID ownerId);
}
