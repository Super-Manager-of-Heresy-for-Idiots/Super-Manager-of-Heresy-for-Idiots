package com.dnd.app.repository;

import com.dnd.app.domain.CampaignMember;
import com.dnd.app.domain.enums.CampaignRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampaignMemberRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    Optional<CampaignMember> findByCampaignIdAndUserId(UUID campaignId, UUID userId);

    List<CampaignMember> findByCampaignIdAndKickedFalse(UUID campaignId);

    List<CampaignMember> findByUserId(UUID userId);

    List<CampaignMember> findByUserIdAndKickedFalse(UUID userId);

    /**
     * Pre-fetches the Campaign association to avoid N+1 lazy loads when callers
     * iterate memberships to access campaign data.
     */
    @Query("select cm from CampaignMember cm join fetch cm.campaign " +
            "where cm.user.id = :userId and cm.kicked = false")
    List<CampaignMember> findByUserIdAndKickedFalseFetchCampaign(@Param("userId") UUID userId);

    long countByCampaignIdAndRoleInCampaignAndKickedFalse(UUID campaignId, CampaignRole role);

    boolean existsByCampaignIdAndUserIdAndKickedFalse(UUID campaignId, UUID userId);
}
