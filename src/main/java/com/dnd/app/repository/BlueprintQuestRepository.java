package com.dnd.app.repository;

import com.dnd.app.domain.BlueprintQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт BlueprintQuestRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BlueprintQuestRepository extends JpaRepository<BlueprintQuest, UUID> {

    List<BlueprintQuest> findByBlueprintId(UUID blueprintId);

    Optional<BlueprintQuest> findByIdAndBlueprintId(UUID id, UUID blueprintId);
}
