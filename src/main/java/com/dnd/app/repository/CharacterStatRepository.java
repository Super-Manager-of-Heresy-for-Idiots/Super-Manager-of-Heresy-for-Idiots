package com.dnd.app.repository;

import com.dnd.app.domain.CharacterStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterStatRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterStatRepository extends JpaRepository<CharacterStat, UUID> {

    List<CharacterStat> findAllByCharacterId(UUID characterId);
}
