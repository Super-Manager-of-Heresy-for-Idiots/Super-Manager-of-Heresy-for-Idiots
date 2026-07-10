package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт HomebrewContentVersionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HomebrewContentVersionRepository extends JpaRepository<HomebrewContentVersion, UUID> {

    List<HomebrewContentVersion> findByHomebrewPackageIdAndVersion(UUID packageId, Integer version);

    List<HomebrewContentVersion> findByHomebrewPackageId(UUID packageId);
}
