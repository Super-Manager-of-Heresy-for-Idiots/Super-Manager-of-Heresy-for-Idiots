package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterFeatureResourceRepository extends JpaRepository<CharacterFeatureResource, UUID> {
    List<CharacterFeatureResource> findByCharacterId(UUID characterId);
    Optional<CharacterFeatureResource> findByCharacterIdAndResourceDefinitionId(UUID characterId, UUID resourceDefinitionId);
    Optional<CharacterFeatureResource> findFirstByCharacterIdAndSharedPoolKey(UUID characterId, String sharedPoolKey);
}
