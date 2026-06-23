package com.dnd.app.repository;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.enums.BattleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleRepository extends JpaRepository<Battle, UUID> {

    List<Battle> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    Optional<Battle> findByIdAndCampaignId(UUID id, UUID campaignId);

    /**
     * Row-locking load used by turn advancement. Serializes concurrent end-turn calls on the same
     * battle (SELECT ... FOR UPDATE): the loser waits, then re-reads the already-advanced turn index
     * and is rejected by the turn-ownership check — so a turn cannot be double-passed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Battle b where b.id = :id and b.campaign.id = :campaignId")
    Optional<Battle> findByIdAndCampaignIdForUpdate(@Param("id") UUID id, @Param("campaignId") UUID campaignId);

    boolean existsByCampaignIdAndStatus(UUID campaignId, BattleStatus status);
}
