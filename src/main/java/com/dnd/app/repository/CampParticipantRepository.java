package com.dnd.app.repository;

import com.dnd.app.domain.CampParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampParticipantRepository описывает репозиторий участников привала.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampParticipantRepository extends JpaRepository<CampParticipant, UUID> {

    List<CampParticipant> findByCampSessionIdOrderByCreatedAtAsc(UUID campSessionId);

    Optional<CampParticipant> findByCampSessionIdAndCharacterId(UUID campSessionId, UUID characterId);

    boolean existsByCampSessionIdAndCharacterId(UUID campSessionId, UUID characterId);

    void deleteByCampSessionIdAndCharacterId(UUID campSessionId, UUID characterId);

    List<CampParticipant> findByActivityId(UUID activityId);
}
