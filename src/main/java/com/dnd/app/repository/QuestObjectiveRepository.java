package com.dnd.app.repository;

import com.dnd.app.domain.QuestObjective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт QuestObjectiveRepository описывает репозиторий опциональных целей квеста
 * (WORLD_PLAN Этап 3). Цели упорядочены по order_index.
 */
public interface QuestObjectiveRepository extends JpaRepository<QuestObjective, UUID> {

    List<QuestObjective> findByQuestIdOrderByOrderIndexAsc(UUID questId);

    long countByQuestId(UUID questId);
}
