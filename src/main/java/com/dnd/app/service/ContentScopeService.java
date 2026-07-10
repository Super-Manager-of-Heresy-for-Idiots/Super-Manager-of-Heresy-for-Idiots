package com.dnd.app.service;

import com.dnd.app.domain.CampaignHomebrew;
import com.dnd.app.repository.CampaignHomebrewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс ContentScopeService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentScopeService {

    private final CampaignHomebrewRepository campaignHomebrewRepository;

    /**
     * Возвращает результат операции "get active package ids" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<UUID> getActivePackageIds(UUID campaignId) {
        return campaignHomebrewRepository.findByCampaignId(campaignId).stream()
                .map(ch -> ch.getHomebrewPackage().getId())
                .toList();
    }

    /**
     * Проверяет условие операции "is package active in campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public boolean isPackageActiveInCampaign(UUID campaignId, UUID packageId) {
        return campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, packageId);
    }

    /**
     * Выполняет операции "activate package" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void activatePackage(UUID campaignId, UUID packageId) {
        // Activation is handled by CampaignHomebrew entity creation
        log.info("Package activated in campaign: campaignId={}, packageId={}", campaignId, packageId);
    }

    /**
     * Выполняет операции "deactivate package" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void deactivatePackage(UUID campaignId, UUID packageId) {
        campaignHomebrewRepository.deleteByCampaignIdAndPackageId(campaignId, packageId);
        log.info("Package deactivated in campaign: campaignId={}, packageId={}", campaignId, packageId);
    }

    /**
     * Возвращает результат операции "get pinned version" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Integer getPinnedVersion(UUID campaignId, UUID packageId) {
        return campaignHomebrewRepository.findByCampaignId(campaignId).stream()
                .filter(ch -> ch.getHomebrewPackage().getId().equals(packageId))
                .findFirst()
                .map(CampaignHomebrew::getPinnedVersion)
                .orElse(null);
    }
}
