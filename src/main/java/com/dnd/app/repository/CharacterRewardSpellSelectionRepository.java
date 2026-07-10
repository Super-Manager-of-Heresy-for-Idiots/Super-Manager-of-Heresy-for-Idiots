package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardSpellSelection;
import com.dnd.app.domain.content.CharacterRewardSpellSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterRewardSpellSelectionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterRewardSpellSelectionRepository
        extends JpaRepository<CharacterRewardSpellSelection, CharacterRewardSpellSelectionId> {

    List<CharacterRewardSpellSelection> findAllBySelectionId(UUID selectionId);
}
