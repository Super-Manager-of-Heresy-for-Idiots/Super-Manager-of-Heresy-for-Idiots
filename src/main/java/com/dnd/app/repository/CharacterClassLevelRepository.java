package com.dnd.app.repository;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterClassLevelId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterClassLevelRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterClassLevelRepository extends JpaRepository<CharacterClassLevel, CharacterClassLevelId> {
    List<CharacterClassLevel> findAllByCharacterId(UUID characterId);
    Optional<CharacterClassLevel> findByCharacterIdAndClassId(UUID characterId, UUID classId);
}
