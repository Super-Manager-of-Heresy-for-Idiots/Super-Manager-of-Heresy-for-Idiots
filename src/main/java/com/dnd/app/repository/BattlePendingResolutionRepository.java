package com.dnd.app.repository;

import com.dnd.app.domain.BattlePendingResolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт BattlePendingResolutionRepository даёт доступ к «отложенным исходам» заклинаний (SAVE_PROMPT).
 * Используется для показа окна у цели и разрешения исхода ответственным пользователем.
 */
public interface BattlePendingResolutionRepository extends JpaRepository<BattlePendingResolution, UUID> {

    /** Все отложенные исходы конкретной цели (обычно 0–1, но при нескольких кастах может быть очередь). */
    List<BattlePendingResolution> findByCombatantIdOrderByCreatedAtAsc(UUID combatantId);

    /** Все отложенные исходы боя (для наполнения ответа по всем комбатантам одним запросом). */
    List<BattlePendingResolution> findByBattleId(UUID battleId);
}
