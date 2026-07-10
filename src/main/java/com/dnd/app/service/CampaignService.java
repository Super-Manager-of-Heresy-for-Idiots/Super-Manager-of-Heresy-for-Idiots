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
import java.util.Map;
import java.util.UUID;

/**
 * Класс CampaignService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final UserRepository userRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final CharacterStatRepository characterStatRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final CharacterWalletRepository characterWalletRepository;
    private final CharacterResourceRepository characterResourceRepository;
    private final MonsterRepository monsterRepository;
    private final CampaignNpcRepository campaignNpcRepository;
    private final com.dnd.app.mapper.CharacterMapper characterMapper;
    private final WebSocketEventService webSocketEventService;

    /**
     * Создает результат операции "create campaign" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get campaign by id" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID id, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceMembershipOrAdmin(campaign, user);
        return toCampaignResponse(campaign, user);
    }

    /**
     * Возвращает результат операции "get campaign detail" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list my campaigns" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Page<CampaignResponse> listMyCampaigns(String username, Pageable pageable) {
        User user = getUser(username);
        if (user.getRole() == Role.ADMIN) {
            Page<Campaign> page = campaignRepository.findAll(pageable);
            return page.map(c -> toCampaignResponse(c, user));
        }
        List<CampaignMember> memberships = campaignMemberRepository.findByUserIdAndKickedFalseFetchCampaign(user.getId());
        List<UUID> campaignIds = memberships.stream().map(m -> m.getCampaign().getId()).toList();
        if (campaignIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
        Page<Campaign> page = campaignRepository.findByIdIn(campaignIds, pageable);
        return page.map(c -> toCampaignResponse(c, user));
    }

    /**
     * Обновляет результат операции "update campaign" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Удаляет результат операции "delete campaign" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteCampaign(UUID id, String username) {
        Campaign campaign = findCampaign(id);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);

        // Campaign-scoped monsters (campaign_id set) are owned by this campaign and die with it.
        // System/homebrew monsters are untouched. monsters.campaign_id has no ON DELETE, so the
        // campaign cannot be removed while they exist; clear NPC references, then drop the monsters.
        campaignNpcRepository.clearSourceMonsterByCampaignId(id);
        monsterRepository.deleteByCampaignId(id);

        log.info("Campaign deleted: id={}, name='{}', by={}", id, campaign.getName(), username);
        campaignRepository.delete(campaign);
    }

    /**
     * Выполняет операции "join campaign" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "leave campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
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

    /**
     * Выполняет операции "kick member" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
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

        // Notify the kicked user directly (their campaign-topic subscription is about to be torn down)
        webSocketEventService.sendUserEvent(target.getUser().getUsername(), WebSocketEventType.MEMBER_KICKED,
                campaignId, Map.of("campaignId", campaignId), creator.getId());
        // Notify the rest of the campaign so they refresh the roster
        webSocketEventService.sendCampaignEvent(WebSocketEventType.MEMBER_KICKED, campaignId,
                Map.of("userId", request.getUserId()), creator.getId());
    }

    /**
     * Выполняет операции "change campaign status" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignResponse changeCampaignStatus(UUID campaignId, ChangeCampaignStatusRequest request, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);

        CampaignStatus newStatus;
        try {
            newStatus = CampaignStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid campaign status: " + request.getStatus(), e);
        }

        campaign.setStatus(newStatus);
        campaign = campaignRepository.save(campaign);
        log.info("Campaign status changed: campaignId={}, newStatus={}, by={}", campaignId, newStatus, username);

        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMPAIGN_STATUS_CHANGED, campaignId,
                Map.of("status", newStatus.name()), user.getId());
        return toCampaignResponse(campaign, user);
    }

    /**
     * Выполняет операции "regenerate invite code" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public InviteCodeResponse regenerateInviteCode(UUID campaignId, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);
        enforceCreatorOrAdmin(campaign, user);

        campaign.setInviteCode(InviteCodeGenerator.generate());
        campaignRepository.save(campaign);
        return InviteCodeResponse.builder().inviteCode(campaign.getInviteCode()).build();
    }

    /**
     * Возвращает результат операции "get invite code" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(UUID campaignId, String username) {
        Campaign campaign = findCampaign(campaignId);
        User user = getUser(username);

        if (!shouldShowInviteCode(campaign, user)) {
            throw new AccessDeniedException("You cannot view the invite code");
        }
        return InviteCodeResponse.builder().inviteCode(campaign.getInviteCode()).build();
    }

    /**
     * Выполняет операции "reassign character" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CharacterResponse reassignCharacter(UUID campaignId, UUID characterId,
                                                ReassignCharacterRequest request, String username) {
        Campaign campaign = findCampaign(campaignId);
        User gmUser = getUser(username);
        enforceGmOrAdmin(campaign, gmUser);

        PlayerCharacter original = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        if (original.getCampaign() == null || !original.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        if (original.getStatus() != CharacterStatus.RESERVE) {
            throw new BadRequestException("Only RESERVE characters can be reassigned");
        }

        User newOwner = userRepository.findById(request.getNewOwnerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("New owner user not found"));
        if (!isMemberOfCampaign(campaignId, newOwner.getId())) {
            throw new BadRequestException("New owner is not a member of this campaign");
        }

        PlayerCharacter copy = PlayerCharacter.builder()
                .name(original.getName())
                .totalLevel(original.getTotalLevel())
                .experience(original.getExperience())
                .status(CharacterStatus.ACTIVE)
                .currentHp(original.getCurrentHp())
                .maxHp(original.getMaxHp())
                .race(original.getRace())
                .owner(newOwner)
                .campaign(campaign)
                .build();
        copy = playerCharacterRepository.saveAndFlush(copy);

        for (CharacterClassLevel ccl : original.getClassLevels()) {
            CharacterClassLevel newCcl = CharacterClassLevel.builder()
                    .characterId(copy.getId())
                    .classId(ccl.getClassId())
                    .classLevel(ccl.getClassLevel())
                    .build();
            classLevelRepository.saveAndFlush(newCcl);
            copy.getClassLevels().add(newCcl);
        }

        for (CharacterStat stat : original.getStats()) {
            CharacterStat newStat = CharacterStat.builder()
                    .character(copy)
                    .statType(stat.getStatType())
                    .value(stat.getValue())
                    .build();
            characterStatRepository.save(newStat);
            copy.getStats().add(newStat);
        }

        for (ItemInstance item : itemInstanceRepository.findByOwnerCharacterId(original.getId())) {
            ItemInstance newItem = ItemInstance.builder()
                    .template(item.getTemplate())
                    .equipmentItem(item.getEquipmentItem())
                    .magicItem(item.getMagicItem())
                    .ownerCharacter(copy)
                    .customName(item.getCustomName())
                    .quantity(item.getQuantity())
                    .isUnique(item.getIsUnique())
                    .slot(item.getSlot())
                    .notes(item.getNotes())
                    .build();
            itemInstanceRepository.save(newItem);
        }

        for (CharacterWallet wallet : characterWalletRepository.findByCharacterId(original.getId())) {
            CharacterWallet newWallet = CharacterWallet.builder()
                    .character(copy)
                    .currencyType(wallet.getCurrencyType())
                    .amount(wallet.getAmount())
                    .build();
            characterWalletRepository.save(newWallet);
        }

        for (CharacterResource resource : characterResourceRepository.findByCharacterId(original.getId())) {
            CharacterResource newResource = CharacterResource.builder()
                    .character(copy)
                    .resourceType(resource.getResourceType())
                    .currentValue(resource.getCurrentValue())
                    .build();
            characterResourceRepository.save(newResource);
        }

        log.info("Character reassigned: originalId={}, copyId={}, newOwner={}, by={}",
                original.getId(), copy.getId(), newOwner.getUsername(), username);
        return characterMapper.toResponse(copy);
    }

    // --- Helper: check if user is GM in campaign ---
    /**
     * Проверяет условие операции "is gm in campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public boolean isGmInCampaign(UUID campaignId, UUID userId) {
        return campaignMemberRepository.findByCampaignIdAndUserId(campaignId, userId)
                .map(m -> !m.getKicked() && m.getRoleInCampaign() == CampaignRole.GM)
                .orElse(false);
    }

    /**
     * Проверяет условие операции "is member of campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public boolean isMemberOfCampaign(UUID campaignId, UUID userId) {
        return campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, userId);
    }

    /**
     * Возвращает результат операции "get membership" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public CampaignMember getMembership(UUID campaignId, UUID userId) {
        return campaignMemberRepository.findByCampaignIdAndUserId(campaignId, userId)
                .filter(m -> !m.getKicked())
                .orElse(null);
    }

    /**
     * Возвращает результат операции "get campaign access" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CampaignAccessResponse getCampaignAccess(UUID campaignId, UUID userId) {
        findCampaign(campaignId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean gm = user.getRole() == Role.ADMIN || isGmInCampaign(campaignId, userId);
        CampaignMember membership = getMembership(campaignId, userId);
        boolean canView = gm || membership != null;

        List<UUID> movableCharacterIds = List.of();
        if (canView && !gm) {
            movableCharacterIds = playerCharacterRepository.findByCampaignIdAndOwnerId(campaignId, userId)
                    .stream()
                    .map(PlayerCharacter::getId)
                    .toList();
        }

        return CampaignAccessResponse.builder()
                .campaignId(campaignId)
                .userId(userId)
                .canView(canView)
                .canManageMaps(gm)
                .canMoveAnyToken(gm)
                .movableCharacterIds(movableCharacterIds)
                .build();
    }

    // --- Access enforcement helpers ---

    /**
     * Проверяет требуемое условие операции "enforce gm or admin" в рамках бизнес-логики домена.
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     * @param user входящее значение user, используемое бизнес-сценарием
     */
    public void enforceGmOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!isGmInCampaign(campaign.getId(), user.getId())) {
            throw new AccessDeniedException("Only GMs of this campaign can perform this action");
        }
    }

    /**
     * Проверяет требуемое условие операции "enforce creator or admin" в рамках бизнес-логики домена.
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     * @param user входящее значение user, используемое бизнес-сценарием
     */
    public void enforceCreatorOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        CampaignMember member = campaignMemberRepository
                .findByCampaignIdAndUserId(campaign.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this campaign"));
        if (!member.getIsCreator()) {
            throw new AccessDeniedException("Only the campaign creator can perform this action");
        }
    }

    /**
     * Проверяет требуемое условие операции "enforce membership or admin" в рамках бизнес-логики домена.
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     * @param user входящее значение user, используемое бизнес-сценарием
     */
    public void enforceMembershipOrAdmin(Campaign campaign, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (!isMemberOfCampaign(campaign.getId(), user.getId())) {
            throw new AccessDeniedException("You are not a member of this campaign");
        }
    }

    /**
     * Проверяет требуемое условие операции "enforce campaign active" в рамках бизнес-логики домена.
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     */
    public void enforceCampaignActive(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BadRequestException("Campaign is not active");
        }
    }

    /**
     * Проверяет требуемое условие операции "enforce campaign active for player" в рамках бизнес-логики домена.
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     * @param user входящее значение user, используемое бизнес-сценарием
     */
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

    /**
     * Находит результат операции "find campaign" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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
