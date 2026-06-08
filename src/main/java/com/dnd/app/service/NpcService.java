package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CreateNoteRequest;
import com.dnd.app.dto.request.CreateNpcRequest;
import com.dnd.app.dto.request.UpdateNoteRequest;
import com.dnd.app.dto.request.UpdateNpcRequest;
import com.dnd.app.dto.response.NoteResponse;
import com.dnd.app.dto.response.NpcResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.NpcNoteRepository;
import com.dnd.app.repository.UserRepository;
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
public class NpcService {

    private final CampaignNpcRepository npcRepository;
    private final NpcNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;

    @Transactional
    public NpcResponse createNpc(UUID campaignId, CreateNpcRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignNpc npc = CampaignNpc.builder()
                .campaign(campaign)
                .name(request.getName())
                .publicDescription(request.getPublicDescription())
                .privateDescription(request.getPrivateDescription())
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();
        npc = npcRepository.save(npc);

        log.info("NPC created: id={}, name='{}', campaignId={}, by={}", npc.getId(), npc.getName(), campaignId, username);
        return toResponse(npc, true);
    }

    @Transactional(readOnly = true)
    public List<NpcResponse> listNpcs(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        boolean isGm = isGmOrAdmin(campaignId, user);
        List<CampaignNpc> npcs;
        if (isGm) {
            npcs = npcRepository.findByCampaignId(campaignId);
        } else {
            npcs = npcRepository.findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
        }
        return npcs.stream().map(npc -> toResponse(npc, isGm)).toList();
    }

    @Transactional(readOnly = true)
    public NpcResponse getNpc(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        boolean isGm = isGmOrAdmin(npc.getCampaign().getId(), user);
        if (!isGm && !Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
        return toResponse(npc, isGm);
    }

    @Transactional
    public NpcResponse updateNpc(UUID npcId, UpdateNpcRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        if (request.getName() != null) npc.setName(request.getName());
        if (request.getPublicDescription() != null) npc.setPublicDescription(request.getPublicDescription());
        if (request.getPrivateDescription() != null) npc.setPrivateDescription(request.getPrivateDescription());
        if (request.getIsVisibleToPlayers() != null) npc.setIsVisibleToPlayers(request.getIsVisibleToPlayers());
        npc = npcRepository.save(npc);

        log.info("NPC updated: id={}, by={}", npcId, username);
        return toResponse(npc, true);
    }

    @Transactional
    public void deleteNpc(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        npcRepository.delete(npc);
        log.info("NPC deleted: id={}, by={}", npcId, username);
    }

    @Transactional
    public NpcResponse toggleVisibility(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        npc.setIsVisibleToPlayers(!Boolean.TRUE.equals(npc.getIsVisibleToPlayers()));
        npc = npcRepository.save(npc);

        log.info("NPC visibility toggled: id={}, visible={}, by={}", npcId, npc.getIsVisibleToPlayers(), username);

        boolean nowVisible = Boolean.TRUE.equals(npc.getIsVisibleToPlayers());
        webSocketEventService.sendCampaignEvent(
                nowVisible ? WebSocketEventType.NPC_REVEALED : WebSocketEventType.NPC_HIDDEN,
                npc.getCampaign().getId(), Map.of("npcId", npcId), user.getId());
        return toResponse(npc, true);
    }

    // --- Notes ---

    @Transactional
    public NoteResponse addNote(UUID npcId, CreateNoteRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);

        boolean isGm = isGmOrAdmin(npc.getCampaign().getId(), user);
        if (!isGm) {
            campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
            if (!Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
                throw new ResourceNotFoundException("NPC not found");
            }
        }

        NpcNote note = NpcNote.builder()
                .npc(npc)
                .author(user)
                .content(request.getContent())
                .build();
        note = noteRepository.save(note);

        log.info("NPC note added: noteId={}, npcId={}, by={}", note.getId(), npcId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, UpdateNoteRequest request, String username) {
        User user = getUser(username);
        NpcNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the note author can update it");
        }

        if (request.getContent() != null) note.setContent(request.getContent());
        note = noteRepository.save(note);

        log.info("NPC note updated: noteId={}, by={}", noteId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public void deleteNote(UUID noteId, String username) {
        User user = getUser(username);
        NpcNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        boolean isGm = isGmOrAdmin(note.getNpc().getCampaign().getId(), user);
        if (!note.getAuthor().getId().equals(user.getId()) && !isGm) {
            throw new AccessDeniedException("Only the note author or a GM can delete it");
        }

        noteRepository.delete(note);
        log.info("NPC note deleted: noteId={}, by={}", noteId, username);
    }

    // --- Private helpers ---

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private CampaignNpc findNpc(UUID npcId) {
        return npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NpcResponse toResponse(CampaignNpc npc, boolean includePrivate) {
        List<NoteResponse> notes = npc.getNotes().stream()
                .map(this::toNoteResponse)
                .toList();

        return NpcResponse.builder()
                .id(npc.getId())
                .name(npc.getName())
                .publicDescription(npc.getPublicDescription())
                .privateDescription(includePrivate ? npc.getPrivateDescription() : null)
                .isVisibleToPlayers(npc.getIsVisibleToPlayers())
                .notes(notes)
                .createdAt(npc.getCreatedAt())
                .updatedAt(npc.getUpdatedAt())
                .build();
    }

    private NoteResponse toNoteResponse(NpcNote note) {
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
