package com.dnd.app.repository;

import com.dnd.app.domain.UserRelationship;
import com.dnd.app.domain.enums.RelationshipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Контракт UserRelationshipRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, UUID> {

    /** Lookup by the already-normalized pair (userAId &lt; userBId). */
    Optional<UserRelationship> findByUserAIdAndUserBId(UUID userAId, UUID userBId);

    /** All relationships in a given status where the user is either side of the pair. */
    @Query("""
            select r from UserRelationship r
            where r.status = :status and (r.userAId = :userId or r.userBId = :userId)
            """)
    List<UserRelationship> findByStatusAndMember(@Param("status") RelationshipStatus status,
                                                 @Param("userId") UUID userId);

    List<UserRelationship> findByStatusAndBlockedById(RelationshipStatus status, UUID blockedById);

    /**
     * Считает заявки в друзья, отправленные пользователем за последние 24 часа (N14, replica-safe
     * backstop). В отличие от per-pod in-memory лимитера, этот потолок не обходится ни рестартом пода,
     * ни размазыванием запросов по репликам. Известное занижение: отменённые/отклонённые строки
     * удаляются и выпадают из счёта — для потолка это консервативно и допустимо.
     * @param requesterId идентификатор пользователя-отправителя
     * @return число заявок, созданных этим отправителем за последние сутки
     */
    @Query(value = """
            select count(*) from user_relationships
             where requester_id = :requesterId and created_at > now() - interval '24 hours'
            """, nativeQuery = true)
    long countRecentByRequester(@Param("requesterId") UUID requesterId);
}
