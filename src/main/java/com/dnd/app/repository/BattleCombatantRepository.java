package com.dnd.app.repository;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.enums.BattleStatus;
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

    /**
     * Every combatant row backing a given character in a battle of the given status. Used to mirror
     * an authoritative HP change from the character sheet onto its live tracker row(s), so a feature
     * that damages/heals a character out of the normal attack flow still updates the combat map.
     */
    List<BattleCombatant> findByCharacter_IdAndBattle_Status(UUID characterId, BattleStatus status);

    boolean existsByBattleIdAndCharacterId(UUID battleId, UUID characterId);

    Optional<BattleCombatant> findByBattleIdAndCharacterId(UUID battleId, UUID characterId);

    /**
     * Row-locking load of a character's combatant in a specific battle, for spending an action-economy
     * slot from outside the core attack flow (feature use / reaction prompts). Same lock semantics as
     * {@link #findByIdForUpdate(UUID)}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BattleCombatant c where c.battle.id = :battleId and c.character.id = :characterId")
    Optional<BattleCombatant> findByBattleIdAndCharacterIdForUpdate(@Param("battleId") UUID battleId,
                                                                    @Param("characterId") UUID characterId);

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
