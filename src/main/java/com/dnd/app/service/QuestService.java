package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.QuestStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CompleteQuestRequest;
import com.dnd.app.dto.request.CreateNoteRequest;
import com.dnd.app.dto.request.CreateQuestRewardRequest;
import com.dnd.app.dto.request.CreateQuestRequest;
import com.dnd.app.dto.request.DistributeXpRequest;
import com.dnd.app.dto.request.GrantItemRequest;
import com.dnd.app.dto.request.ModifyCurrencyRequest;
import com.dnd.app.dto.request.UpdateNoteRequest;
import com.dnd.app.dto.request.UpdateQuestRequest;
import com.dnd.app.dto.response.NoteResponse;
import com.dnd.app.dto.response.QuestCompletionResponse;
import com.dnd.app.dto.response.QuestResponse;
import com.dnd.app.dto.response.QuestRewardResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService {

    private final CampaignQuestRepository questRepository;
    private final QuestNoteRepository noteRepository;
    private final QuestNpcRepository questNpcRepository;
    private final QuestLocationRepository questLocationRepository;
    private final QuestRewardRepository questRewardRepository;
    private final CampaignNpcRepository npcRepository;
    private final CampaignLocationRepository locationRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final ItemInstanceService itemInstanceService;
    private final WalletService walletService;
    private final XpService xpService;

    @Transactional
    public QuestResponse createQuest(UUID campaignId, CreateQuestRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        QuestStatus status = QuestStatus.ACTIVE;
        if (request.getStatus() != null) {
            try {
                status = QuestStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid quest status: " + request.getStatus());
            }
        }

        CampaignQuest quest = CampaignQuest.builder()
                .campaign(campaign)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(status)
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();
        quest = questRepository.save(quest);

        log.info("Quest created: id={}, title='{}', campaignId={}, by={}", quest.getId(), quest.getTitle(), campaignId, username);
        return toResponse(quest, true);
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> listQuests(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        boolean isGm = isGmOrAdmin(campaignId, user);
        List<CampaignQuest> quests;
        if (isGm) {
            quests = questRepository.findByCampaignId(campaignId);
        } else {
            quests = questRepository.findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
        }
        return quests.stream().map(q -> toResponse(q, isGm)).toList();
    }

    @Transactional(readOnly = true)
    public QuestResponse getQuest(UUID questId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceMembershipOrAdmin(quest.getCampaign(), user);

        boolean isGm = isGmOrAdmin(quest.getCampaign().getId(), user);
        if (!isGm && !Boolean.TRUE.equals(quest.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("Quest not found");
        }
        return toResponse(quest, isGm);
    }

    @Transactional
    public QuestResponse updateQuest(UUID questId, UpdateQuestRequest request, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        if (request.getTitle() != null) quest.setTitle(request.getTitle());
        if (request.getDescription() != null) quest.setDescription(request.getDescription());
        if (request.getIsVisibleToPlayers() != null) quest.setIsVisibleToPlayers(request.getIsVisibleToPlayers());
        if (request.getStatus() != null) {
            QuestStatus newStatus;
            try {
                newStatus = QuestStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid quest status: " + request.getStatus());
            }
            if (newStatus == QuestStatus.COMPLETED && quest.getStatus() != QuestStatus.COMPLETED) {
                throw new BadRequestException(
                        "Use the quest completion endpoint to mark a quest COMPLETED and grant its reward");
            }
            quest.setStatus(newStatus);
        }
        quest = questRepository.save(quest);

        log.info("Quest updated: id={}, by={}", questId, username);

        if (Boolean.TRUE.equals(quest.getIsVisibleToPlayers())) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_UPDATED,
                    quest.getCampaign().getId(), Map.of("questId", questId), user.getId());
        }
        return toResponse(quest, true);
    }

    @Transactional
    public void deleteQuest(UUID questId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        questRepository.delete(quest);
        log.info("Quest deleted: id={}, by={}", questId, username);
    }

    // --- Notes ---

    @Transactional
    public NoteResponse addNote(UUID questId, CreateNoteRequest request, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);

        boolean isGm = isGmOrAdmin(quest.getCampaign().getId(), user);
        if (!isGm) {
            campaignService.enforceMembershipOrAdmin(quest.getCampaign(), user);
            if (!Boolean.TRUE.equals(quest.getIsVisibleToPlayers())) {
                throw new ResourceNotFoundException("Quest not found");
            }
        }

        QuestNote note = QuestNote.builder()
                .quest(quest)
                .author(user)
                .content(request.getContent())
                .build();
        note = noteRepository.save(note);

        log.info("Quest note added: noteId={}, questId={}, by={}", note.getId(), questId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, UpdateNoteRequest request, String username) {
        User user = getUser(username);
        QuestNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the note author can update it");
        }

        if (request.getContent() != null) note.setContent(request.getContent());
        note = noteRepository.save(note);

        log.info("Quest note updated: noteId={}, by={}", noteId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public void deleteNote(UUID noteId, String username) {
        User user = getUser(username);
        QuestNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        boolean isGm = isGmOrAdmin(note.getQuest().getCampaign().getId(), user);
        if (!note.getAuthor().getId().equals(user.getId()) && !isGm) {
            throw new AccessDeniedException("Only the note author or a GM can delete it");
        }

        noteRepository.delete(note);
        log.info("Quest note deleted: noteId={}, by={}", noteId, username);
    }

    // --- Rewards ---

    @Transactional
    public QuestRewardResponse addReward(UUID questId, CreateQuestRewardRequest request, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        boolean hasItem = request.getItemTemplateId() != null;
        boolean hasCurrency = request.getCurrencyTypeId() != null;
        boolean hasXp = request.getXpAmount() != null && request.getXpAmount() > 0;
        if (!hasItem && !hasCurrency && !hasXp) {
            throw new BadRequestException("Reward must include an item, currency, or XP");
        }

        QuestReward reward = QuestReward.builder()
                .quest(quest)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .xpAmount(request.getXpAmount())
                .build();

        if (request.getItemTemplateId() != null) {
            ItemTemplate template = itemTemplateRepository.findById(request.getItemTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));
            reward.setItemTemplate(template);
        }
        if (request.getCurrencyTypeId() != null) {
            CurrencyType currencyType = currencyTypeRepository.findById(request.getCurrencyTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Currency type not found"));
            reward.setCurrencyType(currencyType);
            reward.setCurrencyAmount(request.getCurrencyAmount());
        }

        reward = questRewardRepository.save(reward);
        log.info("Quest reward added: rewardId={}, questId={}, by={}", reward.getId(), questId, username);
        return toRewardResponse(reward);
    }

    @Transactional(readOnly = true)
    public List<QuestRewardResponse> listRewards(UUID questId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceMembershipOrAdmin(quest.getCampaign(), user);

        return questRewardRepository.findByQuestId(questId).stream()
                .map(this::toRewardResponse)
                .toList();
    }

    @Transactional
    public void deleteReward(UUID rewardId, String username) {
        User user = getUser(username);
        QuestReward reward = questRewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest reward not found"));
        campaignService.enforceGmOrAdmin(reward.getQuest().getCampaign(), user);

        questRewardRepository.delete(reward);
        log.info("Quest reward deleted: rewardId={}, by={}", rewardId, username);
    }

    // --- Completion & reward issuance ---

    /**
     * Marks a quest COMPLETED and grants its full reward to the chosen recipient
     * character: items are added to the recipient's inventory, currency is
     * credited to the wallet, and XP is added to the character's experience.
     *
     * The recipient must belong to the quest's campaign and is selected at the
     * moment of completion (not at quest creation). The optional {@code xpAmount}
     * on the request overrides the total XP defined on the reward entries.
     *
     * Runs in a single transaction so the status change and all grants are atomic.
     */
    @Transactional
    public QuestCompletionResponse completeQuest(UUID questId, CompleteQuestRequest request, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        Campaign campaign = quest.getCampaign();
        campaignService.enforceGmOrAdmin(campaign, user);

        if (quest.getStatus() == QuestStatus.COMPLETED) {
            throw new BadRequestException("Quest is already completed");
        }

        PlayerCharacter recipient = playerCharacterRepository.findById(request.getRecipientCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient character not found"));
        if (recipient.getCampaign() == null || !recipient.getCampaign().getId().equals(campaign.getId())) {
            throw new BadRequestException("Recipient character does not belong to this campaign");
        }

        UUID campaignId = campaign.getId();
        UUID recipientId = recipient.getId();
        List<QuestReward> rewards = questRewardRepository.findByQuestId(questId);

        int itemsGranted = 0;
        long rewardXp = 0L;
        for (QuestReward reward : rewards) {
            if (reward.getItemTemplate() != null) {
                GrantItemRequest grant = GrantItemRequest.builder()
                        .templateId(reward.getItemTemplate().getId())
                        .quantity(reward.getQuantity() != null ? reward.getQuantity() : 1)
                        .build();
                itemInstanceService.grantItem(campaignId, recipientId, grant, username);
                itemsGranted++;
            }
            if (reward.getCurrencyType() != null && reward.getCurrencyAmount() != null
                    && reward.getCurrencyAmount().signum() > 0) {
                ModifyCurrencyRequest credit = ModifyCurrencyRequest.builder()
                        .currencyTypeId(reward.getCurrencyType().getId())
                        .amount(reward.getCurrencyAmount())
                        .build();
                walletService.modifyCurrency(recipientId, credit, username);
            }
            if (reward.getXpAmount() != null && reward.getXpAmount() > 0) {
                rewardXp += reward.getXpAmount();
            }
        }

        long xpToGrant = request.getXpAmount() != null ? request.getXpAmount() : rewardXp;
        if (xpToGrant > 0) {
            DistributeXpRequest xpRequest = DistributeXpRequest.builder()
                    .amount(xpToGrant)
                    .target("SINGLE")
                    .characterIds(List.of(recipientId))
                    .build();
            xpService.distributeXp(campaignId, xpRequest, username);
        }

        quest.setStatus(QuestStatus.COMPLETED);
        quest = questRepository.save(quest);

        log.info("Quest completed: questId={}, recipientId={}, itemsGranted={}, xpGranted={}, by={}",
                questId, recipientId, itemsGranted, xpToGrant, username);

        if (Boolean.TRUE.equals(quest.getIsVisibleToPlayers())) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.QUEST_UPDATED,
                    campaignId, Map.of("questId", questId, "status", QuestStatus.COMPLETED.name()), user.getId());
        }

        return QuestCompletionResponse.builder()
                .questId(questId)
                .status(quest.getStatus().name())
                .recipientCharacterId(recipientId)
                .recipientCharacterName(recipient.getName())
                .itemsGranted(itemsGranted)
                .xpGranted(xpToGrant)
                .build();
    }

    // --- Link/Unlink NPC ---

    @Transactional
    public void linkNpc(UUID questId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));

        if (!npc.getCampaign().getId().equals(quest.getCampaign().getId())) {
            throw new BadRequestException("NPC does not belong to the same campaign as the quest");
        }

        QuestNpc link = QuestNpc.builder()
                .quest(quest)
                .npc(npc)
                .build();
        questNpcRepository.save(link);

        log.info("NPC linked to quest: questId={}, npcId={}, by={}", questId, npcId, username);
    }

    @Transactional
    public void unlinkNpc(UUID questId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        List<QuestNpc> links = questNpcRepository.findByQuestId(questId);
        QuestNpc link = links.stream()
                .filter(l -> l.getNpc().getId().equals(npcId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("NPC not linked to quest"));

        questNpcRepository.delete(link);
        log.info("NPC unlinked from quest: questId={}, npcId={}, by={}", questId, npcId, username);
    }

    // --- Link/Unlink Location ---

    @Transactional
    public void linkLocation(UUID questId, UUID locationId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        CampaignLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        if (!location.getCampaign().getId().equals(quest.getCampaign().getId())) {
            throw new BadRequestException("Location does not belong to the same campaign as the quest");
        }

        QuestLocation link = QuestLocation.builder()
                .quest(quest)
                .location(location)
                .build();
        questLocationRepository.save(link);

        log.info("Location linked to quest: questId={}, locationId={}, by={}", questId, locationId, username);
    }

    @Transactional
    public void unlinkLocation(UUID questId, UUID locationId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        List<QuestLocation> links = questLocationRepository.findByQuestId(questId);
        QuestLocation link = links.stream()
                .filter(l -> l.getLocation().getId().equals(locationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Location not linked to quest"));

        questLocationRepository.delete(link);
        log.info("Location unlinked from quest: questId={}, locationId={}, by={}", questId, locationId, username);
    }

    // --- Private helpers ---

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private CampaignQuest findQuest(UUID questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private QuestResponse toResponse(CampaignQuest quest, boolean isGm) {
        List<NoteResponse> notes = quest.getNotes().stream()
                .map(this::toNoteResponse)
                .toList();

        return QuestResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .status(quest.getStatus().name())
                .isVisibleToPlayers(quest.getIsVisibleToPlayers())
                .notes(notes)
                .createdAt(quest.getCreatedAt())
                .updatedAt(quest.getUpdatedAt())
                .build();
    }

    private QuestRewardResponse toRewardResponse(QuestReward reward) {
        return QuestRewardResponse.builder()
                .id(reward.getId())
                .itemTemplateId(reward.getItemTemplate() != null ? reward.getItemTemplate().getId() : null)
                .itemTemplateName(reward.getItemTemplate() != null ? reward.getItemTemplate().getName() : null)
                .quantity(reward.getQuantity())
                .currencyTypeId(reward.getCurrencyType() != null ? reward.getCurrencyType().getId() : null)
                .currencyTypeName(reward.getCurrencyType() != null ? reward.getCurrencyType().getNameRu() : null)
                .currencyAmount(reward.getCurrencyAmount())
                .xpAmount(reward.getXpAmount())
                .build();
    }

    private NoteResponse toNoteResponse(QuestNote note) {
        return NoteResponse.builder()
                .id(note.getId())
                .authorId(note.getAuthor().getId())
                .authorUsername(note.getAuthor().getUsername())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
