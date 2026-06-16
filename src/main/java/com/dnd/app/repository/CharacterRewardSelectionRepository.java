package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterRewardSelectionRepository extends JpaRepository<CharacterRewardSelection, UUID> {

    Optional<CharacterRewardSelection> findByCharacterIdAndGroupId(UUID characterId, UUID groupId);

    Optional<CharacterRewardSelection> findByCharacterIdAndOptionId(UUID characterId, UUID optionId);

    List<CharacterRewardSelection> findAllByCharacterId(UUID characterId);
}
