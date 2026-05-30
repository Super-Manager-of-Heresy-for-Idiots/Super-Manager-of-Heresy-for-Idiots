package com.dnd.app.repository;

import com.dnd.app.domain.QuestNpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestNpcRepository extends JpaRepository<QuestNpc, UUID> {

    List<QuestNpc> findByQuestId(UUID questId);
}
