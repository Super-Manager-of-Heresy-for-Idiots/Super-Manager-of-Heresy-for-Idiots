package com.dnd.app.repository;

import com.dnd.app.domain.CampEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampEventRepository описывает репозиторий журнала привала.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampEventRepository extends JpaRepository<CampEvent, UUID> {

    List<CampEvent> findByCampSessionIdOrderByCreatedAtAsc(UUID campSessionId);

    List<CampEvent> findByCampSessionIdAndVisibleToPlayersTrueOrderByCreatedAtAsc(UUID campSessionId);

    Optional<CampEvent> findByIdAndCampSessionId(UUID id, UUID campSessionId);
}
