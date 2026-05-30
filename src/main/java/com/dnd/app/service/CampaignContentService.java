package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.ActivateHomebrewRequest;
import com.dnd.app.dto.request.UpdatePinnedVersionRequest;
import com.dnd.app.dto.response.CampaignHomebrewResponse;
import com.dnd.app.dto.response.TeamAvailableContentResponse;
import com.dnd.app.dto.response.TeamAvailableContentResponse.AvailableContentItem;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignContentService {

    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

    @Transactional
    public CampaignHomebrewResponse activateHomebrew(UUID campaignId, ActivateHomebrewRequest request, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        HomebrewPackage pkg = homebrewPackageRepository.findById(request.getHomebrewPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew-пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Homebrew-пакет не найден или не опубликован");
        }

        if (campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, pkg.getId())) {
            throw new DuplicateResourceException("Этот пакет уже активирован для данной кампании");
        }

        CampaignHomebrew activation = CampaignHomebrew.builder()
                .campaignId(campaignId)
                .packageId(pkg.getId())
                .build();
        campaignHomebrewRepository.save(activation);

        log.info("Homebrew activated: package='{}', campaignId={}, by user={}", pkg.getTitle(), campaignId, username);

        return buildResponse(pkg, null);
    }

    @Transactional
    public void deactivateHomebrew(UUID campaignId, UUID packageId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        if (!campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, packageId)) {
            throw new ResourceNotFoundException("Активация пакета не найдена для этой кампании");
        }

        campaignHomebrewRepository.deleteByCampaignIdAndPackageId(campaignId, packageId);
        log.info("Homebrew deactivated: packageId={}, campaignId={}, by user={}", packageId, campaignId, username);
    }

    @Transactional
    public CampaignHomebrewResponse updatePinnedVersion(UUID campaignId, UUID packageId,
                                                         UpdatePinnedVersionRequest request, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignHomebrewId id = new CampaignHomebrewId(campaignId, packageId);
        CampaignHomebrew activation = campaignHomebrewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package is not attached to this campaign"));

        HomebrewPackage pkg = homebrewPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));

        if (request.getPinnedVersion() != null && request.getPinnedVersion() > pkg.getVersion()) {
            throw new com.dnd.app.exception.BadRequestException(
                    "Pinned version cannot exceed the latest published version (" + pkg.getVersion() + ")");
        }

        activation.setPinnedVersion(request.getPinnedVersion());
        campaignHomebrewRepository.save(activation);

        log.info("Pinned version updated: packageId={}, campaignId={}, pinnedVersion={}, by={}",
                packageId, campaignId, request.getPinnedVersion(), username);
        return buildResponse(pkg, request.getPinnedVersion());
    }

    @Transactional(readOnly = true)
    public List<CampaignHomebrewResponse> listActiveHomebrew(UUID campaignId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        List<CampaignHomebrew> activations = campaignHomebrewRepository.findByCampaignId(campaignId);
        return activations.stream().map(a -> {
            HomebrewPackage pkg = a.getHomebrewPackage();
            return buildResponse(pkg, a.getPinnedVersion());
        }).toList();
    }

    @Transactional(readOnly = true)
    public TeamAvailableContentResponse getAvailableContent(UUID campaignId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        Set<UUID> activePackageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<AvailableContentItem> classes = new ArrayList<>();
        classRepository.findAllBySourceHomebrewIsNull().forEach(c ->
                classes.add(AvailableContentItem.builder()
                        .id(c.getId()).name(c.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            classRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(c ->
                    classes.add(AvailableContentItem.builder()
                            .id(c.getId()).name(c.getName()).source("HOMEBREW")
                            .homebrewTitle(c.getSourceHomebrew() != null ? c.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> races = raceRepository.findAll().stream()
                .map(r -> AvailableContentItem.builder()
                        .id(r.getId()).name(r.getName()).source("GLOBAL").build())
                .toList();

        List<AvailableContentItem> itemTypes = new ArrayList<>();
        itemTypeRepository.findAllBySourceHomebrewIsNull().forEach(it ->
                itemTypes.add(AvailableContentItem.builder()
                        .id(it.getId()).name(it.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            itemTypeRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(it ->
                    itemTypes.add(AvailableContentItem.builder()
                            .id(it.getId()).name(it.getName()).source("HOMEBREW")
                            .homebrewTitle(it.getSourceHomebrew() != null ? it.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> skills = new ArrayList<>();
        skillRepository.findAllBySourceHomebrewIsNull().forEach(s ->
                skills.add(AvailableContentItem.builder()
                        .id(s.getId()).name(s.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            skillRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(s ->
                    skills.add(AvailableContentItem.builder()
                            .id(s.getId()).name(s.getName()).source("HOMEBREW")
                            .homebrewTitle(s.getSourceHomebrew() != null ? s.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> feats = new ArrayList<>();
        featRepository.findAllBySourceHomebrewIsNull().forEach(f ->
                feats.add(AvailableContentItem.builder()
                        .id(f.getId()).name(f.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            featRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(f ->
                    feats.add(AvailableContentItem.builder()
                            .id(f.getId()).name(f.getName()).source("HOMEBREW")
                            .homebrewTitle(f.getSourceHomebrew() != null ? f.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        return TeamAvailableContentResponse.builder()
                .classes(classes).races(races).itemTypes(itemTypes).skills(skills).feats(feats)
                .build();
    }

    public boolean isClassAvailableInCampaign(UUID campaignId, UUID classId) {
        CharacterClass cc = classRepository.findById(classId).orElse(null);
        if (cc == null) return false;
        if (cc.getSourceHomebrew() == null) return true;
        Set<UUID> activePackageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        return activePackageIds.contains(cc.getSourceHomebrew().getId());
    }

    public boolean isRaceAvailableInCampaign(UUID campaignId, UUID raceId) {
        return raceRepository.existsById(raceId);
    }

    private CampaignHomebrewResponse buildResponse(HomebrewPackage pkg, Integer pinnedVersion) {
        List<Object[]> countsByType = contentItemRepository.countByPackageGroupedByType(pkg.getId());
        Map<String, Long> contentSummary = new LinkedHashMap<>();
        for (Object[] row : countsByType) {
            ContentType ct = (ContentType) row[0];
            Long count = (Long) row[1];
            contentSummary.put(ct.name().toLowerCase() + "Count", count);
        }
        return CampaignHomebrewResponse.builder()
                .packageId(pkg.getId())
                .title(pkg.getTitle())
                .pinnedVersion(pinnedVersion)
                .contentSummary(contentSummary)
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
