package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CampaignAccessResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAccessResponse {

    private UUID campaignId;
    private UUID userId;
    private boolean canView;
    private boolean canManageMaps;
    private boolean canMoveAnyToken;
    private List<UUID> movableCharacterIds;
}
