package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterKnownForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterKnownFormRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterKnownFormRepository extends JpaRepository<CharacterKnownForm, UUID> {
    List<CharacterKnownForm> findByCharacterId(UUID characterId);
    boolean existsByCharacterIdAndMonsterId(UUID characterId, UUID monsterId);
}
