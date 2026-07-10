package com.dnd.app.repository;

import com.dnd.app.domain.SharedStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт SharedStorageRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SharedStorageRepository extends JpaRepository<SharedStorage, UUID> {

    List<SharedStorage> findByCampaignId(UUID campaignId);
}
