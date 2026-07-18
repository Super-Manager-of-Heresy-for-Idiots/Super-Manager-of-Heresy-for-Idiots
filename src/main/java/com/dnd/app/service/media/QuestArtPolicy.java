package com.dnd.app.service.media;

import com.dnd.app.domain.CampaignQuest;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignQuestRepository;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс QuestArtPolicy — политика прав на арт квеста кампании ({@code QUEST_ART}).
 * Права как у NPC/локации: менять может GM кампании (или ADMIN), читать — участники кампании (или ADMIN).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class QuestArtPolicy implements MediaOwnerPolicy {

    private final CampaignQuestRepository questRepository;
    private final CampaignService campaignService;

    /** @return тип владельца — арт квеста */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.QUEST_ART;
    }

    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        UUID campaignId = requireQuest(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isGmInCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Менять арт квеста может только GM кампании.");
        }
    }

    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        UUID campaignId = requireQuest(ownerId).getCampaign().getId();
        if (!user.isAdmin() && !campaignService.isMemberOfCampaign(campaignId, user.id())) {
            throw new AccessDeniedException("Арт квеста доступен только участникам кампании.");
        }
    }

    /**
     * Находит квест или бросает 404.
     * @param id идентификатор квеста
     * @return сущность квеста
     */
    private CampaignQuest requireQuest(UUID id) {
        return questRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден."));
    }
}
