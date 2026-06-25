package com.dnd.app.repository;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.enums.CombatantType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleCombatantRepository extends JpaRepository<BattleCombatant, UUID> {

    List<BattleCombatant> findByBattleIdOrderByTurnOrderAsc(UUID battleId);

    boolean existsByBattleIdAndCharacterId(UUID battleId, UUID characterId);

    long countByBattleIdAndTypeAndMonsterId(UUID battleId, CombatantType type, UUID monsterId);

    /**
     * Row-locking load (SELECT ... FOR UPDATE) for mutations that change a combatant's HP or
     * action-economy flags, so concurrent attacks / HP edits on the same combatant serialize
     * instead of racing. Callers must already hold the battle lock first (consistent lock order:
     * battle → combatant) to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BattleCombatant c where c.id = :id")
    Optional<BattleCombatant> findByIdForUpdate(@Param("id") UUID id);
}
