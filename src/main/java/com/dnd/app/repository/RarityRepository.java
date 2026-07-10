package com.dnd.app.repository;

import com.dnd.app.domain.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт RarityRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface RarityRepository extends JpaRepository<Rarity, UUID> {

    List<Rarity> findByHomebrewIsNullOrderBySortOrderAscNameRuAsc();

    Optional<Rarity> findBySlugAndHomebrewIsNull(String slug);

    Optional<Rarity> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
