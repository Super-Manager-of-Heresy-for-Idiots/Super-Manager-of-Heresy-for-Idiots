package com.dnd.app.repository;

import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.enums.CampStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampSessionRepository описывает репозиторий привалов кампании.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampSessionRepository extends JpaRepository<CampSession, UUID> {

    List<CampSession> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    Optional<CampSession> findByIdAndCampaignId(UUID id, UUID campaignId);

    /**
     * Возвращает текущий незавершённый привал кампании (инвариант — не более одного).
     * @param campaignId идентификатор кампании
     * @return активный привал или пусто
     */
    @Query("select c from CampSession c where c.campaign.id = :campaignId and c.status <> :completed")
    Optional<CampSession> findCurrent(@Param("campaignId") UUID campaignId,
                                      @Param("completed") CampStatus completed);

    /**
     * Загрузка привала под блокировкой строки: сериализует конкурентные переходы статуса
     * и запуск группового отдыха, чтобы отдых не стартовал дважды.
     * @param id идентификатор привала
     * @param campaignId идентификатор кампании
     * @return привал или пусто
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CampSession c where c.id = :id and c.campaign.id = :campaignId")
    Optional<CampSession> findByIdAndCampaignIdForUpdate(@Param("id") UUID id,
                                                         @Param("campaignId") UUID campaignId);
}
