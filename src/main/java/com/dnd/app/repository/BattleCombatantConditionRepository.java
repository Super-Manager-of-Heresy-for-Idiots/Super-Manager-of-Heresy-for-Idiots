package com.dnd.app.repository;

import com.dnd.app.domain.BattleCombatantCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BattleCombatantConditionRepository extends JpaRepository<BattleCombatantCondition, UUID> {

    List<BattleCombatantCondition> findByCombatantId(UUID combatantId);

    Optional<BattleCombatantCondition> findByCombatantIdAndConditionId(UUID combatantId, UUID conditionId);

    /** Every condition on any combatant of the battle — for the round-boundary tick and DTO projection. */
    List<BattleCombatantCondition> findByCombatant_Battle_Id(UUID battleId);
}
