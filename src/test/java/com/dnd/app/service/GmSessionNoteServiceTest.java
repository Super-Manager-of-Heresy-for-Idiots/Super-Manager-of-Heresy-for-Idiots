package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.GmSessionNote;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateGmNoteRequest;
import com.dnd.app.dto.response.GmSessionNoteResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.repository.GmSessionNoteRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GmSessionNoteServiceTest {

    @Mock private GmSessionNoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;

    @InjectMocks private GmSessionNoteService gmSessionNoteService;

    private UUID campaignId;
    private UUID noteId;
    private User gmUser;
    private User playerUser;
    private Campaign campaign;
    private GmSessionNote note;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        noteId = UUID.randomUUID();

        gmUser = User.builder()
                .id(UUID.randomUUID())
                .username("gm1")
                .role(Role.GAME_MASTER)
                .build();

        playerUser = User.builder()
                .id(UUID.randomUUID())
                .username("player1")
                .role(Role.PLAYER)
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .name("Test Campaign")
                .build();

        note = GmSessionNote.builder()
                .id(noteId)
                .campaign(campaign)
                .author(gmUser)
                .title("Session 1")
                .content("Notes...")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void listNotes_gmCanList() {
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gmUser));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceGmOrAdmin(campaign, gmUser);
        when(noteRepository.findByCampaignId(campaignId)).thenReturn(List.of(note));

        List<GmSessionNoteResponse> result = gmSessionNoteService.listNotes(campaignId, "gm1");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Session 1", result.get(0).getTitle());
        verify(campaignService).enforceGmOrAdmin(campaign, gmUser);
    }

    @Test
    void listNotes_playerCannotList() {
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(playerUser));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doThrow(new AccessDeniedException("Only GMs of this campaign can perform this action"))
                .when(campaignService).enforceGmOrAdmin(campaign, playerUser);

        assertThrows(AccessDeniedException.class,
                () -> gmSessionNoteService.listNotes(campaignId, "player1"));

        verify(noteRepository, never()).findByCampaignId(any());
    }

    @Test
    void createNote_gmCanCreate() {
        CreateGmNoteRequest request = CreateGmNoteRequest.builder()
                .title("Session 1")
                .content("Notes...")
                .build();

        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gmUser));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceGmOrAdmin(campaign, gmUser);
        when(noteRepository.save(any(GmSessionNote.class))).thenReturn(note);

        GmSessionNoteResponse result = gmSessionNoteService.createNote(campaignId, request, "gm1");

        assertNotNull(result);
        assertEquals(noteId, result.getId());
        assertEquals(campaignId, result.getCampaignId());
        assertEquals("gm1", result.getAuthorUsername());
        assertEquals("Session 1", result.getTitle());
        assertEquals("Notes...", result.getContent());
        verify(noteRepository).save(any(GmSessionNote.class));
    }

    @Test
    void createNote_playerCannotCreate() {
        CreateGmNoteRequest request = CreateGmNoteRequest.builder()
                .title("Session 1")
                .content("Notes...")
                .build();

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(playerUser));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doThrow(new AccessDeniedException("Only GMs of this campaign can perform this action"))
                .when(campaignService).enforceGmOrAdmin(campaign, playerUser);

        assertThrows(AccessDeniedException.class,
                () -> gmSessionNoteService.createNote(campaignId, request, "player1"));

        verify(noteRepository, never()).save(any());
    }

    @Test
    void getNote_playerCannotView() {
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(playerUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        doThrow(new AccessDeniedException("Only GMs of this campaign can perform this action"))
                .when(campaignService).enforceGmOrAdmin(note.getCampaign(), playerUser);

        assertThrows(AccessDeniedException.class,
                () -> gmSessionNoteService.getNote(noteId, "player1"));
    }

    @Test
    void deleteNote_gmCanDelete() {
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gmUser));
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        doNothing().when(campaignService).enforceGmOrAdmin(note.getCampaign(), gmUser);

        gmSessionNoteService.deleteNote(noteId, "gm1");

        verify(noteRepository).delete(note);
    }
}
