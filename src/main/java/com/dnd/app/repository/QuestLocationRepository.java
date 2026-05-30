package com.dnd.app.repository;

import com.dnd.app.domain.QuestLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestLocationRepository extends JpaRepository<QuestLocation, UUID> {

    List<QuestLocation> findByQuestId(UUID questId);
}
