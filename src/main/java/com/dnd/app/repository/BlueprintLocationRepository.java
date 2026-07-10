package com.dnd.app.repository;

import com.dnd.app.domain.BlueprintLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт BlueprintLocationRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BlueprintLocationRepository extends JpaRepository<BlueprintLocation, UUID> {

    List<BlueprintLocation> findByBlueprintId(UUID blueprintId);

    Optional<BlueprintLocation> findByIdAndBlueprintId(UUID id, UUID blueprintId);
}
