package com.dnd.app.repository;

import com.dnd.app.domain.CreatureSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CreatureSizeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CreatureSizeRepository extends JpaRepository<CreatureSize, UUID> {

    List<CreatureSize> findByHomebrewIsNullOrderByNameRuAsc();

    Optional<CreatureSize> findBySlugAndHomebrewIsNull(String slug);

    Optional<CreatureSize> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
