package com.dnd.app.repository;

import com.dnd.app.domain.CurrencyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CurrencyTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CurrencyTypeRepository extends JpaRepository<CurrencyType, UUID> {

    List<CurrencyType> findByHomebrewIsNull();

    List<CurrencyType> findByHomebrewIdIn(List<UUID> ids);

    Optional<CurrencyType> findBySlugAndHomebrewIsNull(String slug);
}
