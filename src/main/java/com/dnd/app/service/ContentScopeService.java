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
 * Resolves which homebrew packages are active for a given campaign.
 * Content is in scope when: homebrew_id IS NULL (vanilla)
 * OR homebrew_id IN (active packages for the campaign).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentScopeService {

    private final CampaignHomebrewRepository campaignHomebrewRepository;

    @Transactional(readOnly = true)
    public List<UUID> getActivePackageIds(UUID campaignId) {
        return campaignHomebrewRepository.findByCampaignId(campaignId).stream()
                .map(ch -> ch.getHomebrewPackage().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isPackageActiveInCampaign(UUID campaignId, UUID packageId) {
        return campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, packageId);
    }

    @Transactional
    public void activatePackage(UUID campaignId, UUID packageId) {
        // Activation is handled by CampaignHomebrew entity creation
        log.info("Package activated in campaign: campaignId={}, packageId={}", campaignId, packageId);
    }

    @Transactional
    public void deactivatePackage(UUID campaignId, UUID packageId) {
        campaignHomebrewRepository.deleteByCampaignIdAndPackageId(campaignId, packageId);
        log.info("Package deactivated in campaign: campaignId={}, packageId={}", campaignId, packageId);
    }

    /**
     * Get pinned version for a package in a campaign, or null if not pinned.
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
