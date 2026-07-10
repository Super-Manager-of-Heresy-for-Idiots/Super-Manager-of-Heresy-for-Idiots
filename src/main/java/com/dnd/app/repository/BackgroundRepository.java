package com.dnd.app.repository;

import com.dnd.app.domain.Background;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт BackgroundRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BackgroundRepository extends JpaRepository<Background, UUID> {

    boolean existsByNameRu(String nameRu);

    List<Background> findAllByHomebrewIsNull();

    List<Background> findAllByHomebrewIdIn(Set<UUID> packageIds);
}
