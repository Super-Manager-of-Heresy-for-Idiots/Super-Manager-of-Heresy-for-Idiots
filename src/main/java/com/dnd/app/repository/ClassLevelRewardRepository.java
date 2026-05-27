package com.dnd.app.repository;

import com.dnd.app.domain.ClassLevelReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardRepository extends JpaRepository<ClassLevelReward, UUID> {
    List<ClassLevelReward> findAllByCharacterClassId(UUID classId);
    List<ClassLevelReward> findAllByCharacterClassIdAndRequiredLevel(UUID classId, Integer requiredLevel);
}
