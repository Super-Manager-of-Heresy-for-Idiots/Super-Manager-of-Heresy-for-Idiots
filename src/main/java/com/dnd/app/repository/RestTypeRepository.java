package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.RestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Контракт RestTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface RestTypeRepository extends JpaRepository<RestType, UUID> {
    Optional<RestType> findByCode(String code);
}
