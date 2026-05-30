package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.QuestStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateNoteRequest;
import com.dnd.app.dto.request.CreateQuestRequest;
import com.dnd.app.dto.request.UpdateNoteRequest;
import com.dnd.app.dto.request.UpdateQuestRequest;
import com.dnd.app.dto.response.NoteResponse;
import com.dnd.app.dto.response.QuestResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService {

    private final CampaignQuestRepository questRepository;
    private final QuestNoteRepository noteRepository;
    private final QuestNpcRepository questNpcRepository;
    private final QuestLocationRepository questLocationRepository;
    private final CampaignNpcRepository npcRepository;
    private final CampaignLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

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
                // default to ACTIVE
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
            try {
                quest.setStatus(QuestStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore invalid status
            }
        }
        quest = questRepository.save(quest);

        log.info("Quest updated: id={}, by={}", questId, username);
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

    // --- Link/Unlink NPC ---

    @Transactional
    public void linkNpc(UUID questId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignQuest quest = findQuest(questId);
        campaignService.enforceGmOrAdmin(quest.getCampaign(), user);

        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));

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
