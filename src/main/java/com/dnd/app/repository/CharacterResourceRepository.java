package com.dnd.app.repository;

import com.dnd.app.domain.CharacterResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterResourceRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterResourceRepository extends JpaRepository<CharacterResource, UUID> {

    List<CharacterResource> findByCharacterId(UUID characterId);

    Optional<CharacterResource> findByCharacterIdAndResourceTypeId(UUID characterId, UUID resourceTypeId);

    long deleteByResourceTypeId(UUID resourceTypeId);
}
