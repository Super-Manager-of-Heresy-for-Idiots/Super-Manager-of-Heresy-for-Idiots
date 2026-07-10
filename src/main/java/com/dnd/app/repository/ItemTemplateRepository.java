package com.dnd.app.repository;

import com.dnd.app.domain.ItemTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ItemTemplateRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ItemTemplateRepository extends JpaRepository<ItemTemplate, UUID> {

    List<ItemTemplate> findByHomebrewIsNull();

    List<ItemTemplate> findByHomebrewIdIn(List<UUID> homebrewIds);

    Page<ItemTemplate> findByHomebrewIsNullOrHomebrewIdIn(List<UUID> homebrewIds, Pageable pageable);
}
