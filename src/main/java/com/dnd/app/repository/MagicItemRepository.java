package com.dnd.app.repository;

import com.dnd.app.domain.content.MagicItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт MagicItemRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface MagicItemRepository extends JpaRepository<MagicItem, UUID> {

    List<MagicItem> findAllByHomebrewIsNull();

    List<MagicItem> findAllByHomebrewIdIn(Set<UUID> packageIds);

    // --- P1.5 / IT-2: авторинг magic-item в пакете ---
    java.util.Optional<MagicItem> findByIdAndHomebrew_Id(UUID id, UUID homebrewId);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
