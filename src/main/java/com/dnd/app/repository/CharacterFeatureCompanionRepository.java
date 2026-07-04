package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterFeatureCompanion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterFeatureCompanionRepository extends JpaRepository<CharacterFeatureCompanion, UUID> {
    List<CharacterFeatureCompanion> findByCharacterId(UUID characterId);
}
