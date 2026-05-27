package com.dnd.app.repository;

import com.dnd.app.domain.CharacterAcquiredReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterAcquiredRewardRepository extends JpaRepository<CharacterAcquiredReward, UUID> {
    List<CharacterAcquiredReward> findAllByCharacterId(UUID characterId);
    boolean existsByCharacterIdAndClassLevelRewardId(UUID characterId, UUID classLevelRewardId);
}
