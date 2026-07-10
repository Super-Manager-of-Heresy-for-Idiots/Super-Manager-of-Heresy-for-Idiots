package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт ActionTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ActionTypeRepository extends JpaRepository<ActionType, UUID> {
    Optional<ActionType> findByCode(String code);
    List<ActionType> findByIdIn(Collection<UUID> ids);
}
