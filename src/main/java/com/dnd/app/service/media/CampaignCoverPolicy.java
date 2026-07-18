package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс CampaignCoverPolicy — политика прав на обложку кампании ({@code CAMPAIGN_COVER}).
 * Владельцем является сама кампания, поэтому {@code ownerId} == campaignId (без косвенности).
 * Менять обложку может GM кампании (или ADMIN); читать — участники кампании (или ADMIN).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class CampaignCoverPolicy implements MediaOwnerPolicy {

    private final CampaignService campaignService;

    /** @return тип владельца — обложка кампании */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.CAMPAIGN_COVER;
    }

    /**
     * Разрешает менять обложку только GM кампании или ADMIN (кампания должна существовать).
     * @param ownerId идентификатор кампании
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        campaignService.findCampaign(ownerId); // 404, если кампании нет
        if (!user.isAdmin() && !campaignService.isGmInCampaign(ownerId, user.id())) {
            throw new AccessDeniedException("Менять обложку кампании может только GM.");
        }
    }

    /**
     * Разрешает просмотр обложки участникам кампании или ADMIN.
     * @param ownerId идентификатор кампании
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        if (!user.isAdmin() && !campaignService.isMemberOfCampaign(ownerId, user.id())) {
            throw new AccessDeniedException("Обложка кампании доступна только участникам.");
        }
    }
}
