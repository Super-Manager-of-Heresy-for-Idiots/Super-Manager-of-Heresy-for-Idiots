package com.dnd.app.repository;

import com.dnd.app.domain.DamageType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт DamageTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface DamageTypeRepository extends DictionaryRepository<DamageType> {

    List<DamageType> findByHomebrewIsNullOrderByNameRuAsc();

    Optional<DamageType> findBySlugAndHomebrewIsNull(String slug);

    Optional<DamageType> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
