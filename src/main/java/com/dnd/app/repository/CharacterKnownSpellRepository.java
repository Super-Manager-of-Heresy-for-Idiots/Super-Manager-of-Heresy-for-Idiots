package com.dnd.app.repository;

import com.dnd.app.domain.CharacterKnownSpell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterKnownSpellRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterKnownSpellRepository extends JpaRepository<CharacterKnownSpell, UUID> {

    List<CharacterKnownSpell> findByCharacterId(UUID characterId);

    Optional<CharacterKnownSpell> findByCharacterIdAndSpellId(UUID characterId, UUID spellId);

    boolean existsByCharacterIdAndSpellId(UUID characterId, UUID spellId);
}
