package com.dnd.app.repository;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterClassLevelId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterClassLevelRepository extends JpaRepository<CharacterClassLevel, CharacterClassLevelId> {
    List<CharacterClassLevel> findAllByCharacterId(UUID characterId);
    Optional<CharacterClassLevel> findByCharacterIdAndClassId(UUID characterId, UUID classId);
}
