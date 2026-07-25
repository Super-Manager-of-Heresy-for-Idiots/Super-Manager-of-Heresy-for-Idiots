package com.dnd.app.repository;

import com.dnd.app.domain.CampActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampActivityRepository описывает репозиторий даунтайм-активностей привала.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampActivityRepository extends JpaRepository<CampActivity, UUID> {

    /**
     * Возвращает справочник активностей, доступных кампании: системные плюс её собственные.
     * @param campaignId идентификатор кампании
     * @return список активностей в порядке "системные, затем кастомные по имени"
     */
    @Query("select a from CampActivity a where a.campaign is null or a.campaign.id = :campaignId "
            + "order by case when a.campaign is null then 0 else 1 end, a.name")
    List<CampActivity> findAvailableForCampaign(@Param("campaignId") UUID campaignId);

    /**
     * Находит активность, доступную кампании (системную или её собственную).
     * @param id идентификатор активности
     * @param campaignId идентификатор кампании
     * @return активность или пусто
     */
    @Query("select a from CampActivity a where a.id = :id and (a.campaign is null or a.campaign.id = :campaignId)")
    Optional<CampActivity> findAvailableForCampaign(@Param("id") UUID id, @Param("campaignId") UUID campaignId);

    Optional<CampActivity> findByIdAndCampaignId(UUID id, UUID campaignId);
}
