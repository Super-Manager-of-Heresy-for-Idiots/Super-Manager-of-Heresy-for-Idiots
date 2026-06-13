package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateNpcRequest;
import com.dnd.app.dto.response.NpcResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.NpcNoteRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NpcService: видимость NPC для игроков и ГМ")
class NpcServiceTest {

    @Mock private CampaignNpcRepository npcRepository;
    @Mock private NpcNoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;

    @InjectMocks private NpcService npcService;

    // --- Helper factories ---

    private User buildPlayer() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("player1")
                .role(Role.PLAYER)
                .build();
    }

    private User buildGm() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("gm1")
                .role(Role.GAME_MASTER)
                .build();
    }

    private Campaign buildCampaign() {
        return Campaign.builder()
                .id(UUID.randomUUID())
                .name("Test Campaign")
                .build();
    }

    private CampaignNpc buildNpc(Campaign campaign, User createdBy,
                                  boolean visible, String publicDesc, String privateDesc) {
        return CampaignNpc.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .name("Test NPC")
                .isVisibleToPlayers(visible)
                .publicDescription(publicDesc)
                .privateDescription(privateDesc)
                .createdBy(createdBy)
                .notes(new ArrayList<>())
                .build();
    }

    // --- Tests ---

    @Test
    @DisplayName("Игрок видит только видимых NPC")
    void listNpcs_playerOnlySeesVisibleNpcs() {
        User player = buildPlayer();
        Campaign campaign = buildCampaign();
        UUID campaignId = campaign.getId();

        CampaignNpc visibleNpc = buildNpc(campaign, buildGm(), true, "public info", null);

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceMembershipOrAdmin(campaign, player);
        when(campaignService.isGmInCampaign(campaignId, player.getId())).thenReturn(false);
        when(npcRepository.findByCampaignIdAndIsVisibleToPlayersTrue(campaignId))
                .thenReturn(List.of(visibleNpc));

        List<NpcResponse> result = npcService.listNpcs(campaignId, "player1");

        assertEquals(1, result.size());
        assertEquals(visibleNpc.getName(), result.get(0).getName());
        verify(npcRepository, never()).findByCampaignId(campaignId);
        verify(npcRepository).findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
    }

    @Test
    @DisplayName("ГМ видит всех NPC, включая скрытых")
    void listNpcs_gmSeesAllNpcs() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        UUID campaignId = campaign.getId();

        CampaignNpc visibleNpc = buildNpc(campaign, gm, true, "public", "secret");
        CampaignNpc hiddenNpc = buildNpc(campaign, gm, false, "hidden public", "hidden secret");

        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceMembershipOrAdmin(campaign, gm);
        when(campaignService.isGmInCampaign(campaignId, gm.getId())).thenReturn(true);
        when(npcRepository.findByCampaignId(campaignId))
                .thenReturn(List.of(visibleNpc, hiddenNpc));

        List<NpcResponse> result = npcService.listNpcs(campaignId, "gm1");

        assertEquals(2, result.size());
        verify(npcRepository).findByCampaignId(campaignId);
        verify(npcRepository, never()).findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
    }

    @Test
    @DisplayName("Игрок не может получить доступ к скрытому NPC")
    void getNpc_playerCannotAccessHiddenNpc() {
        User player = buildPlayer();
        Campaign campaign = buildCampaign();
        CampaignNpc hiddenNpc = buildNpc(campaign, buildGm(), false, "public", "secret");

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(npcRepository.findById(hiddenNpc.getId())).thenReturn(Optional.of(hiddenNpc));
        doNothing().when(campaignService).enforceMembershipOrAdmin(campaign, player);
        when(campaignService.isGmInCampaign(campaign.getId(), player.getId())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> npcService.getNpc(hiddenNpc.getId(), "player1"));
    }

    @Test
    @DisplayName("Игрок видит видимого NPC без приватного описания")
    void getNpc_playerSeesVisibleNpc_noPrivateDescription() {
        User player = buildPlayer();
        Campaign campaign = buildCampaign();
        CampaignNpc visibleNpc = buildNpc(campaign, buildGm(), true, "public info", "secret");

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(npcRepository.findById(visibleNpc.getId())).thenReturn(Optional.of(visibleNpc));
        doNothing().when(campaignService).enforceMembershipOrAdmin(campaign, player);
        when(campaignService.isGmInCampaign(campaign.getId(), player.getId())).thenReturn(false);

        NpcResponse response = npcService.getNpc(visibleNpc.getId(), "player1");

        assertEquals("public info", response.getPublicDescription());
        assertNull(response.getPrivateDescription());
    }

    @Test
    @DisplayName("ГМ видит приватное описание NPC")
    void getNpc_gmSeesPrivateDescription() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        CampaignNpc npc = buildNpc(campaign, gm, true, "public info", "secret");

        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(npcRepository.findById(npc.getId())).thenReturn(Optional.of(npc));
        doNothing().when(campaignService).enforceMembershipOrAdmin(campaign, gm);
        when(campaignService.isGmInCampaign(campaign.getId(), gm.getId())).thenReturn(true);

        NpcResponse response = npcService.getNpc(npc.getId(), "gm1");

        assertEquals("public info", response.getPublicDescription());
        assertEquals("secret", response.getPrivateDescription());
    }

    @Test
    @DisplayName("Игрок не может создать NPC")
    void createNpc_playerCannotCreate() {
        User player = buildPlayer();
        Campaign campaign = buildCampaign();
        UUID campaignId = campaign.getId();

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doThrow(new AccessDeniedException("Only GMs of this campaign can perform this action"))
                .when(campaignService).enforceGmOrAdmin(campaign, player);

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Evil NPC")
                .publicDescription("looks evil")
                .build();

        assertThrows(AccessDeniedException.class,
                () -> npcService.createNpc(campaignId, request, "player1"));

        verify(npcRepository, never()).save(any());
    }
}
