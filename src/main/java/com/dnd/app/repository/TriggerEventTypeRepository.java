package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.TriggerEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Контракт TriggerEventTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface TriggerEventTypeRepository extends JpaRepository<TriggerEventType, UUID> {
    Optional<TriggerEventType> findByCode(String code);
}
