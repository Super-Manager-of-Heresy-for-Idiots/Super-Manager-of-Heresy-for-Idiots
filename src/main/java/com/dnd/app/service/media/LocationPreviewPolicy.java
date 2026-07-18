package com.dnd.app.service.media;

import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс LocationPreviewPolicy — политика прав на превью локации кампании ({@code LOCATION_PREVIEW}).
 * Права как у NPC: загрузку/замену/удаление разрешает GM кампании (или ADMIN), чтение — участникам
 * кампании (или ADMIN).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class LocationPreviewPolicy implements MediaOwnerPolicy {

    private final CampaignLocationRepository locationRepository;
    private final CampaignService campaignService;

    /** @return тип владельца — превью локации */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.LOCATION_PREVIEW;
    }

    /**
     * Разрешает менять превью только GM кампании локации или ADMIN.
     * @param ownerId идентификатор локации
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        UUID campaignId = requireLocation(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isGmInCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Менять превью локации может только GM кампании.");
        }
    }

    /**
     * Разрешает просмотр превью участникам кампании локации или ADMIN.
     * @param ownerId идентификатор локации
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        UUID campaignId = requireLocation(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isMemberOfCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Превью локации доступно только участникам кампании.");
        }
    }

    /**
     * Находит локацию или бросает 404.
     * @param id идентификатор локации
     * @return сущность локации
     */
    private CampaignLocation requireLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена."));
    }
}
