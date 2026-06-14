package com.dnd.app.repository;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.enums.CombatantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BattleCombatantRepository extends JpaRepository<BattleCombatant, UUID> {

    List<BattleCombatant> findByBattleIdOrderByTurnOrderAsc(UUID battleId);

    boolean existsByBattleIdAndCharacterId(UUID battleId, UUID characterId);

    long countByBattleIdAndTypeAndMonsterId(UUID battleId, CombatantType type, UUID monsterId);
}
