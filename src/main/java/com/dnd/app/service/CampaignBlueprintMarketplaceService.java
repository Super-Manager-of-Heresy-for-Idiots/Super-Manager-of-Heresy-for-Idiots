package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BlueprintStatus;
import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.domain.enums.CampaignStatus;
import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.InstantiateBlueprintRequest;
import com.dnd.app.dto.response.CampaignBlueprintDetailResponse;
import com.dnd.app.dto.response.CampaignBlueprintResponse;
import com.dnd.app.dto.response.CampaignResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignBlueprintMarketplaceService {

    private final CampaignBlueprintRepository blueprintRepository;
    private final CampaignBlueprintService blueprintService;

    private final BlueprintNpcRepository npcRepository;
    private final BlueprintQuestRepository questRepository;
    private final BlueprintLocationRepository locationRepository;
    private final BlueprintHomebrewRepository blueprintHomebrewRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final PlayerCharacterRepository playerCharacterRepository;

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignNpcRepository campaignNpcRepository;
    private final CampaignQuestRepository campaignQuestRepository;
    private final CampaignLocationRepository campaignLocationRepository;
    private final CampaignService campaignService;

    // ============================ Marketplace read (PLAYER+) ============================

    @Transactional(readOnly = true)
    public Page<CampaignBlueprintResponse> browseMarketplace(String search, String universeSlug,
                                                             String sort, int page, int size, String username) {
        // Any authenticated user (incl. PLAYER) may browse the blueprint marketplace.
        blueprintService.getUser(username);
        Pageable pageable = buildPageable(sort, page, size);
        String s = (search == null || search.isBlank()) ? null : search.trim();
        String u = (universeSlug == null || universeSlug.isBlank()) ? null : universeSlug.trim();

        Page<CampaignBlueprint> blueprints;
        if (s != null && u != null) {
            blueprints = blueprintRepository.findMarketplaceBySearchAndUniverseSlug(s, u, pageable);
        } else if (s != null) {
            blueprints = blueprintRepository.findMarketplaceBySearch(s, pageable);
        } else if (u != null) {
            blueprints = blueprintRepository.findMarketplaceByUniverseSlug(u, pageable);
        } else {
            blueprints = blueprintRepository.findMarketplace(pageable);
        }

        return blueprints.map(blueprintService::toResponse);
    }

    @Transactional(readOnly = true)
    public CampaignBlueprintDetailResponse getMarketplaceBlueprint(UUID id, String username) {
        blueprintService.getUser(username);
        CampaignBlueprint blueprint = blueprintRepository.findPublishedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон кампании не найден"));
        return blueprintService.toDetailResponse(blueprint);
    }

    // ============================ Fork (GM/ADMIN) ============================

    @Transactional
    public CampaignBlueprintDetailResponse fork(UUID id, String username) {
        User caller = blueprintService.getAuthoringUser(username);
        CampaignBlueprint original = blueprintService.findBlueprint(id);
        if (original.isDeleted()) {
            throw new ResourceNotFoundException("Шаблон кампании не найден");
        }
        if (original.getStatus() != BlueprintStatus.PUBLISHED) {
            throw new BadRequestException("Форкнуть можно только опубликованный шаблон");
        }
        if (Boolean.FALSE.equals(original.getAllowForks())) {
            throw new AccessDeniedException("Автор запретил создавать форки этого шаблона");
        }

        CampaignBlueprint copy = CampaignBlueprint.builder()
                .author(caller)
                .parent(original)
                .originVersion(original.getVersion())
                .universe(original.getUniverse())
                .title(original.getTitle())
                .loreDescription(original.getLoreDescription())
                .coverUrl(original.getCoverUrl())
                .allowForks(original.getAllowForks())
                .status(BlueprintStatus.DRAFT)
                .version(1)
                .build();
        copy = blueprintRepository.save(copy);

        for (BlueprintNpc n : npcRepository.findByBlueprintId(original.getId())) {
            BlueprintNpc nc = BlueprintNpc.builder()
                    .blueprint(copy)
                    .name(n.getName())
                    .publicDescription(n.getPublicDescription())
                    .privateDescription(n.getPrivateDescription())
                    .isVisibleToPlayers(n.getIsVisibleToPlayers())
                    .createdBy(caller)
                    .sourceType(n.getSourceType())
                    .race(n.getRace())
                    .characterClass(n.getCharacterClass())
                    .level(n.getLevel())
                    .abilities(n.getAbilities())
                    .sourceMonster(n.getSourceMonster())
                    .spells(new LinkedHashSet<>(n.getSpells()))
                    .build();
            npcRepository.save(nc);
        }

        for (BlueprintQuest q : questRepository.findByBlueprintId(original.getId())) {
            BlueprintQuest qc = BlueprintQuest.builder()
                    .blueprint(copy)
                    .title(q.getTitle())
                    .description(q.getDescription())
                    .status(q.getStatus())
                    .isVisibleToPlayers(q.getIsVisibleToPlayers())
                    .createdBy(caller)
                    .build();
            for (BlueprintReward r : q.getRewards()) {
                BlueprintReward rc = BlueprintReward.builder()
                        .quest(qc)
                        .itemTemplate(r.getItemTemplate())
                        .quantity(r.getQuantity())
                        .currencyType(r.getCurrencyType())
                        .currencyAmount(r.getCurrencyAmount())
                        .xpAmount(r.getXpAmount())
                        .build();
                qc.getRewards().add(rc);
            }
            questRepository.save(qc);
        }

        for (BlueprintLocation l : locationRepository.findByBlueprintId(original.getId())) {
            BlueprintLocation lc = BlueprintLocation.builder()
                    .blueprint(copy)
                    .name(l.getName())
                    .description(l.getDescription())
                    .isVisibleToPlayers(l.getIsVisibleToPlayers())
                    .createdBy(caller)
                    .build();
            locationRepository.save(lc);
        }

        for (BlueprintHomebrew bh : blueprintHomebrewRepository.findByBlueprintId(original.getId())) {
            BlueprintHomebrew bhc = BlueprintHomebrew.builder()
                    .blueprintId(copy.getId())
                    .packageId(bh.getPackageId())
                    .pinnedVersion(bh.getPinnedVersion())
                    .build();
            blueprintHomebrewRepository.save(bhc);
        }

        for (PlayerCharacter pc : playerCharacterRepository.findByBlueprintId(original.getId())) {
            blueprintService.copyCharacter(pc, caller, null, copy, pc.getStatus());
        }

        original.setDownloadCount(original.getDownloadCount() + 1);
        blueprintRepository.save(original);

        log.info("Campaign blueprint forked: originalId={}, copyId={}, by={}", original.getId(), copy.getId(), username);
        return blueprintService.toDetailResponse(copy);
    }

    // ============================ Instantiate into a Campaign ============================

    @Transactional
    public CampaignResponse instantiate(UUID id, InstantiateBlueprintRequest request, String username) {
        User caller = blueprintService.getUser(username);
        CampaignBlueprint blueprint = blueprintService.findBlueprint(id);
        if (blueprint.isDeleted()) {
            throw new ResourceNotFoundException("Шаблон кампании не найден");
        }
        enforceInstantiateAccess(blueprint, caller);

        // 1) Create the campaign with the caller as GM creator.
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inviteCode(InviteCodeGenerator.generate())
                .status(CampaignStatus.ACTIVE)
                .build();
        campaign = campaignRepository.save(campaign);

        CampaignMember creator = CampaignMember.builder()
                .campaign(campaign)
                .user(caller)
                .roleInCampaign(CampaignRole.GM)
                .isCreator(true)
                .build();
        campaignMemberRepository.save(creator);

        // 2) Homebrew links — carry pinned version, falling back to the package's current version.
        for (BlueprintHomebrew bh : blueprintHomebrewRepository.findByBlueprintId(id)) {
            Integer pinned = bh.getPinnedVersion();
            if (pinned == null) {
                HomebrewPackage pkg = homebrewPackageRepository.findById(bh.getPackageId()).orElse(null);
                pinned = pkg != null ? pkg.getVersion() : null;
            }
            campaignHomebrewRepository.save(CampaignHomebrew.builder()
                    .campaignId(campaign.getId())
                    .packageId(bh.getPackageId())
                    .pinnedVersion(pinned)
                    .build());
        }

        // 3) NPCs
        for (BlueprintNpc n : npcRepository.findByBlueprintId(id)) {
            CampaignNpc cn = CampaignNpc.builder()
                    .campaign(campaign)
                    .name(n.getName())
                    .isVisibleToPlayers(n.getIsVisibleToPlayers())
                    .publicDescription(n.getPublicDescription())
                    .privateDescription(n.getPrivateDescription())
                    .createdBy(caller)
                    .sourceType(n.getSourceType())
                    .race(n.getRace())
                    .characterClass(n.getCharacterClass())
                    .level(n.getLevel())
                    .abilities(n.getAbilities())
                    .sourceMonster(n.getSourceMonster())
                    .spells(new LinkedHashSet<>(n.getSpells()))
                    .build();
            campaignNpcRepository.save(cn);
        }

        // 4) Quests + rewards (rewards persist via cascade on the quest)
        for (BlueprintQuest q : questRepository.findByBlueprintId(id)) {
            CampaignQuest cq = CampaignQuest.builder()
                    .campaign(campaign)
                    .title(q.getTitle())
                    .description(q.getDescription())
                    .status(q.getStatus())
                    .isVisibleToPlayers(q.getIsVisibleToPlayers())
                    .createdBy(caller)
                    .build();
            for (BlueprintReward r : q.getRewards()) {
                QuestReward qr = QuestReward.builder()
                        .quest(cq)
                        .itemTemplate(r.getItemTemplate())
                        .quantity(r.getQuantity())
                        .currencyType(r.getCurrencyType())
                        .currencyAmount(r.getCurrencyAmount())
                        .xpAmount(r.getXpAmount())
                        .build();
                cq.getRewards().add(qr);
            }
            campaignQuestRepository.save(cq);
        }

        // 5) Locations
        for (BlueprintLocation l : locationRepository.findByBlueprintId(id)) {
            campaignLocationRepository.save(CampaignLocation.builder()
                    .campaign(campaign)
                    .name(l.getName())
                    .description(l.getDescription())
                    .isVisibleToPlayers(l.getIsVisibleToPlayers())
                    .createdBy(caller)
                    .build());
        }

        // 6) Pre-built characters → deep copy into the campaign as RESERVE.
        for (PlayerCharacter pc : playerCharacterRepository.findByBlueprintId(id)) {
            blueprintService.copyCharacter(pc, caller, campaign, null, CharacterStatus.RESERVE);
        }

        log.info("Campaign instantiated from blueprint: blueprintId={}, campaignId={}, by={}",
                id, campaign.getId(), username);
        return campaignService.getCampaignById(campaign.getId(), username);
    }

    // ============================ Helpers ============================

    private void enforceInstantiateAccess(CampaignBlueprint blueprint, User caller) {
        if (caller.getRole() == Role.ADMIN) {
            return;
        }
        boolean isAuthor = blueprint.getAuthor().getId().equals(caller.getId());
        if (isAuthor) {
            return;
        }
        if (caller.getRole() != Role.GAME_MASTER) {
            throw new AccessDeniedException("Только мастера игры могут создавать кампании из шаблона");
        }
        if (blueprint.getStatus() != BlueprintStatus.PUBLISHED) {
            throw new AccessDeniedException("Создавать кампанию можно только из опубликованного шаблона");
        }
    }

    private Pageable buildPageable(String sort, int page, int size) {
        Sort sortOrder = switch (sort != null ? sort.toLowerCase() : "newest") {
            case "downloads" -> Sort.by(Sort.Direction.DESC, "downloadCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, size, sortOrder);
    }
}
