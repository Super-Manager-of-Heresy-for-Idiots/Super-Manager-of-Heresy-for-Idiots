package com.dnd.app.repository;

import com.dnd.app.domain.BlueprintNpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт BlueprintNpcRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BlueprintNpcRepository extends JpaRepository<BlueprintNpc, UUID> {

    List<BlueprintNpc> findByBlueprintId(UUID blueprintId);

    Optional<BlueprintNpc> findByIdAndBlueprintId(UUID id, UUID blueprintId);
}
