package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.*;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.*;
import com.dnd.app.repository.*;
import com.dnd.app.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final UserRepository userRepository;
    private final PlayerCharacterRepository playerCharacterRepository;

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Game Masters can create campaigns");
        }

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inviteCode(InviteCodeGenerator.generate())
                .status(CampaignStatus.ACTIVE)
                .build();
        campaign = campaignRepository.save(campaign);

        CampaignMember creator = CampaignMember.builder()
                .campaign(campaign)
                .user(user)
                .roleInCampaign(CampaignRole.GM)
                .isCreator(true)
                .build();
        campaignMemberRepository.save(creator);

        log.info("Campaign created: id={}, name='{}', creator={}", campaign.getId(), campaign.getName(), username);
        return toCampaignResponse(campaign, user);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID id, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceMembershipOrAdmin(campaign, user);
        return toCampaignResponse(campaign, user);
    }

    @Transactional(readOnly = true)
    public CampaignDetailResponse getCampaignDetail(UUID id, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceMembershipOrAdmin(campaign, user);

        List<CampaignMember> members = campaignMemberRepository.findByCampaignIdAndKickedFalse(id);
        List<CampaignMemberResponse> memberResponses = members.stream()
                .map(this::toMemberResponse)
                .toList();

        boolean showInviteCode = shouldShowInviteCode(campaign, user);

        return CampaignDetailResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .status(campaign.getStatus().name())
                .inviteCode(showInviteCode ? campaign.getInviteCode() : null)
                .memberCount(members.size())
                .members(memberResponses)
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> listMyCampaigns(String username, Pageable pageable) {
        User user = getUser(username);
        if (user.getRole() == Role.ADMIN) {
            Page<Campaign> page = campaignRepository.findAll(pageable);
            return page.map(c -> toCampaignResponse(c, user));
        }
        List<CampaignMember> memberships = campaignMemberRepository.findByUserIdAndKickedFalse(user.getId());
        List<UUID> campaignIds = memberships.stream().map(m -> m.getCampaign().getId()).toList();
        if (campaignIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        Page<Campaign> page = campaignRepository.findByIdIn(campaignIds, pageable);
        return page.map(c -> toCampaignResponse(c, user));
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID id, UpdateCampaignRequest request, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceGmOrAdmin(campaign, user);

        if (request.getName() != null) campaign.setName(request.getName());
        if (request.getDescription() != null) campaign.setDescription(request.getDescription());
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign, user);
    }

    @Transactional
    public void deleteCampaign(UUID id, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);
        log.info("Campaign deleted: id={}, name='{}', by={}", id, campaign.getName(), username);
        campaignRepository.delete(campaign);
    }

    @Transactional
    public CampaignResponse joinCampaign(JoinCampaignRequest request, String username) {
        User user = getUser(username);
        if (user.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Admins cannot join campaigns");
        }

        Campaign campaign = campaignRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code"));

        if (campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaign.getId(), user.getId())) {
            throw new DuplicateResourceException("You are already a member of this campaign");
        }

        // Check if kicked
        campaignMemberRepository.findByCampaignIdAndUserId(campaign.getId(), user.getId())
                .ifPresent(existing -> {
                    if (existing.getKicked()) {
                        throw new AccessDeniedException("You have been kicked from this campaign");
                    }
                });

        CampaignRole role = (user.getRole() == Role.GAME_MASTER) ? CampaignRole.GM : CampaignRole.PLAYER;
        CampaignMember member = CampaignMember.builder()
                .campaign(campaign)
                .user(user)
                .roleInCampaign(role)
                .isCreator(false)
                .build();
        campaignMemberRepository.save(member);

        log.info("User joined campaign: user={}, role={}, campaignId={}", username, role, campaign.getId());
        return toCampaignResponse(campaign, user);
    }

    @Transactional
    public void leaveCampaign(UUID campaignId, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);

        CampaignMember member = campaignMemberRepository
                .findByCampaignIdAndUserId(campaignId, user.getId())
                .orElseThrow(() -> new BadRequestException("You are not a member of this campaign"));

        if (member.getKicked()) {
            throw new BadRequestException("You have already been removed from this campaign");
        }

        if (member.getIsCreator()) {
            throw new BadRequestException("Campaign creator cannot leave. Transfer ownership or delete the campaign.");
        }

        // If player leaves, set their characters to RESERVE
        if (member.getRoleInCampaign() == CampaignRole.PLAYER) {
            List<PlayerCharacter> characters = playerCharacterRepository
                    .findByCampaignIdAndOwnerId(campaignId, user.getId());
            for (PlayerCharacter pc : characters) {
                pc.setStatus(CharacterStatus.RESERVE);
            }
            playerCharacterRepository.saveAll(characters);
        }

        campaignMemberRepository.delete(member);
        log.info("User left campaign: user={}, campaignId={}", username, campaignId);
    }

    @Transactional
    public void kickMember(UUID campaignId, KickMemberRequest request, String username) {
        Campaign campaign = findCampaign(campaignId);
        User creator = getUser(username);
        enforceCreatorOrAdmin(campaign, creator);

        CampaignMember target = campaignMemberRepository
                .findByCampaignIdAndUserId(campaignId, request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in campaign"));

        if (target.getIsCreator()) {
            throw new BadRequestException("Cannot kick the campaign creator");
        }

        target.setKicked(true);
        campaignMemberRepository.save(target);

        // Regenerate invite code so kicked user can't rejoin
        campaign.setInviteCode(InviteCodeGenerator.generate());
        campaignRepository.save(campaign);

        // Set kicked player's characters to RESERVE
        if (target.getRoleInCampaign() == CampaignRole.PLAYER) {
            List<PlayerCharacter> characters = playerCharacterRepository
                    .findByCampaignIdAndOwnerId(campaignId, request.getUserId());
            for (PlayerCharacter pc : characters) {
                pc.setStatus(CharacterStatus.RESERVE);
            }
            playerCharacterRepository.saveAll(characters);
        }

        log.info("Member kicked from campaign: userId={}, campaignId={}, by={}", request.getUserId(), campaignId, username);
    }

    @Transactional
    public CampaignResponse changeCampaignStatus(UUID campaignId, ChangeCampaignStatusRequest request, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);

        CampaignStatus newStatus;
        try {
            newStatus = CampaignStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid campaign status: " + request.getStatus());
        }

        campaign.setStatus(newStatus);
        campaign = campaignRepository.save(campaign);
        log.info("Campaign status changed: campaignId={}, newStatus={}, by={}", campaignId, newStatus, username);
        return toCampaignResponse(campaign, user);
    }

    @Transactional
    public InviteCodeResponse regenerateInviteCode(UUID campaignId, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);

        campaign.setInviteCode(InviteCodeGenerator.generate());
        campaignRepository.save(campaign);
        return InviteCodeResponse.builder().inviteCode(campaign.getInviteCode()).build();
    }

    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(UUID campaignId, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);

        if (!shouldShowInviteCode(campaign, user)) {
            throw new AccessDeniedException("You cannot view the invite code");
        }
        return InviteCodeResponse.builder().inviteCode(campaign.getInviteCode()).build();
    }

    // --- Helper: check if user is GM in campaign ---
    public boolean isGmInCampaign(UUID campaignId, UUID userId) {
        return campaignMemberRepository.findByCampaignIdAndUserId(campaignId, userId)
                .map(m -> !m.getKicked() && m.getRoleInCampaign() == CampaignRole.GM)
                .orElse(false);
    }

    public boolean isMemberOfCampaign(UUID campaignId, UUID userId) {
        return campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, userId);
    }

    public CampaignMember getMembership(UUID campaignId, UUID userId) {
        return campaignMemberRepository.findByCampaignIdAndUserId(campaignId, userId)
                .filter(m -> !m.getKicked())
                .orElse(null);
    }

    // --- Access enforcement helpers ---

    public void enforceGmOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!isGmInCampaign(campaign.getId(), user.getId())) {
            throw new AccessDeniedException("Only GMs of this campaign can perform this action");
        }
    }

    public void enforceCreatorOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        CampaignMember member = campaignMemberRepository
                .findByCampaignIdAndUserId(campaign.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this campaign"));
        if (!member.getIsCreator()) {
            throw new AccessDeniedException("Only the campaign creator can perform this action");
        }
    }

    public void enforceMembershipOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!isMemberOfCampaign(campaign.getId(), user.getId())) {
            throw new AccessDeniedException("You are not a member of this campaign");
        }
    }

    public void enforceCampaignActive(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BadRequestException("Campaign is not active");
        }
    }

    public void enforceCampaignActiveForPlayer(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (campaign.getStatus() != CampaignStatus.ACTIVE && user.getRole() == Role.PLAYER) {
            throw new BadRequestException("Campaign is paused or completed — read-only for players");
        }
    }

    // --- Private helpers ---

    private boolean shouldShowInviteCode(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return true;
        CampaignMember membership = getMembership(campaign.getId(), user.getId());
        if (membership == null) return false;
        if (membership.getRoleInCampaign() == CampaignRole.GM) return true;
        // Players see invite code only if 0 GMs in campaign
        long gmCount = campaignMemberRepository
                .countByCampaignIdAndRoleInCampaignAndKickedFalse(campaign.getId(), CampaignRole.GM);
        return gmCount == 0;
    }

    public Campaign findCampaign(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CampaignResponse toCampaignResponse(Campaign campaign, User requestingUser) {
        boolean showInviteCode = shouldShowInviteCode(campaign, requestingUser);
        List<CampaignMember> members = campaignMemberRepository.findByCampaignIdAndKickedFalse(campaign.getId());
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .status(campaign.getStatus().name())
                .inviteCode(showInviteCode ? campaign.getInviteCode() : null)
                .memberCount(members.size())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }

    private CampaignMemberResponse toMemberResponse(CampaignMember member) {
        return CampaignMemberResponse.builder()
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .roleInCampaign(member.getRoleInCampaign().name())
                .isCreator(member.getIsCreator())
                .joinedAt(member.getJoinedAt())
                .kicked(member.getKicked())
                .build();
    }
}
