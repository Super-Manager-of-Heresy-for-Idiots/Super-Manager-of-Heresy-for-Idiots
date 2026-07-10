package com.dnd.app.repository;

import com.dnd.app.domain.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт ItemTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ItemTypeRepository extends JpaRepository<ItemType, UUID> {

    boolean existsByName(String name);

    List<ItemType> findAllByHomebrewIsNull();

    List<ItemType> findAllByHomebrewIdIn(Set<UUID> packageIds);
}
