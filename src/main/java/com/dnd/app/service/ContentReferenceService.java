package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only reference access for the new normalized content model. Replaces the
 * class-reading behaviour of {@link ReferenceDataService} for the final
 * {@link ContentClassDetailResponse} shape.
 *
 * <p>Campaign-aware visibility = core content ({@code homebrew_id IS NULL}) plus the
 * homebrew packages activated for the campaign. Vanilla variants expose core content
 * only (used by character templates without a campaign).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentReferenceService {

    private final ContentCharacterClassRepository classRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;
    private final ContentClassMapper classMapper;

    // --- campaign-aware ---

    @Transactional(readOnly = true)
    public List<ContentClassDetailResponse> getCampaignClasses(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<ContentCharacterClass> classes = new ArrayList<>(classRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            classes.addAll(classRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return classes.stream().map(c -> classMapper.toDetail(c, resolvedLang)).toList();
    }

    @Transactional(readOnly = true)
    public ContentClassDetailResponse getCampaignClass(UUID campaignId, UUID classId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        ContentCharacterClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        enforceVisibleInCampaign(campaignId, clazz);
        return classMapper.toDetail(clazz, resolvedLang);
    }

    // --- vanilla / core only ---

    @org.springframework.cache.annotation.Cacheable(
            value = com.dnd.app.config.CacheConfig.CONTENT_VANILLA_CLASSES, key = "#lang")
    @Transactional(readOnly = true)
    public List<ContentClassDetailResponse> getVanillaClasses(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return classRepository.findAllByHomebrewIsNull().stream()
                .map(c -> classMapper.toDetail(c, resolvedLang))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentClassDetailResponse getVanillaClass(UUID classId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        ContentCharacterClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (clazz.getHomebrew() != null) {
            throw new ResourceNotFoundException("Class not found");
        }
        return classMapper.toDetail(clazz, resolvedLang);
    }

    // --- access helpers ---

    private void enforceAccess(UUID campaignId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
    }

    private void enforceVisibleInCampaign(UUID campaignId, ContentCharacterClass clazz) {
        if (clazz.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(clazz.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Class not found");
        }
    }
}
