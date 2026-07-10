package com.dnd.app.repository;

import com.dnd.app.domain.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт StatTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface StatTypeRepository extends JpaRepository<StatType, UUID> {

    List<StatType> findByHomebrewIsNull();

    List<StatType> findByHomebrewIdIn(List<UUID> ids);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
