package com.dnd.app.repository;

import com.dnd.app.domain.content.EquipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт EquipmentItemRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, UUID> {

    List<EquipmentItem> findAllByHomebrewIsNull();

    List<EquipmentItem> findAllByHomebrewIdIn(Set<UUID> packageIds);
}
