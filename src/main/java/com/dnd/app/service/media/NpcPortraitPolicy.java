package com.dnd.app.service.media;

import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс NpcPortraitPolicy — политика прав на портрет NPC кампании ({@code NPC_PORTRAIT}).
 * Загрузку/замену/удаление разрешает только GM кампании этого NPC (или ADMIN); чтение — участникам
 * кампании (или ADMIN). Переиспощает те же проверки campaign-доступа, что и {@code NpcService}
 * ({@code isGmInCampaign} / {@code isMemberOfCampaign}), чтобы права портрета совпадали с правами на сам NPC.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class NpcPortraitPolicy implements MediaOwnerPolicy {

    private final CampaignNpcRepository npcRepository;
    private final CampaignService campaignService;

    /** @return тип владельца, обслуживаемый политикой — портрет NPC */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.NPC_PORTRAIT;
    }

    /**
     * Разрешает менять портрет только GM кампании NPC или ADMIN.
     * @param ownerId идентификатор NPC
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        UUID campaignId = requireNpc(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isGmInCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Менять портрет NPC может только GM кампании.");
        }
    }

    /**
     * Разрешает просмотр портрета участникам кампании NPC или ADMIN.
     * @param ownerId идентификатор NPC
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        UUID campaignId = requireNpc(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isMemberOfCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Портрет NPC доступен только участникам кампании.");
        }
    }

    /**
     * Находит NPC или бросает 404.
     * @param npcId идентификатор NPC
     * @return сущность NPC кампании
     */
    private CampaignNpc requireNpc(UUID npcId) {
        return npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC не найден."));
    }
}
