package com.dnd.app.repository;

import com.dnd.app.domain.CharacterFeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterFeatRepository extends JpaRepository<CharacterFeat, UUID> {

    List<CharacterFeat> findByCharacterId(UUID characterId);

    Optional<CharacterFeat> findByCharacterIdAndFeatId(UUID characterId, UUID featId);

    boolean existsByCharacterIdAndFeatId(UUID characterId, UUID featId);
}
