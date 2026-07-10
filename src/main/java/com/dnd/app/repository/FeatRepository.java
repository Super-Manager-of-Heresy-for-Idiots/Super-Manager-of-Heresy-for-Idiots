package com.dnd.app.repository;

import com.dnd.app.domain.Feat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт FeatRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatRepository extends JpaRepository<Feat, UUID> {
    boolean existsByNameRu(String nameRu);

    List<Feat> findAllByHomebrewIsNull();

    List<Feat> findAllByHomebrewIdIn(Set<UUID> packageIds);
}
