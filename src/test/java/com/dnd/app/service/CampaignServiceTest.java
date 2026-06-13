package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.*;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.*;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignService: управление участниками и статусом кампании")
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignMemberRepository campaignMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerCharacterRepository playerCharacterRepository;

    @Mock
    private WebSocketEventService webSocketEventService;

    @InjectMocks
    private CampaignService campaignService;

    private UUID campaignId;
    private UUID creatorId;
    private UUID targetId;
    private User creatorUser;
    private User targetUser;
    private Campaign campaign;
    private CampaignMember creatorMember;
    private CampaignMember targetMember;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        targetId = UUID.randomUUID();

        creatorUser = User.builder()
                .id(creatorId)
                .username("creator")
                .role(Role.GAME_MASTER)
                .build();

        targetUser = User.builder()
                .id(targetId)
                .username("target")
                .role(Role.PLAYER)
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .name("Test Campaign")
                .description("A test campaign")
                .status(CampaignStatus.ACTIVE)
                .inviteCode("OLD_CODE")
                .build();

        creatorMember = CampaignMember.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .user(creatorUser)
                .roleInCampaign(CampaignRole.GM)
                .isCreator(true)
                .kicked(false)
                .build();

        targetMember = CampaignMember.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .user(targetUser)
                .roleInCampaign(CampaignRole.PLAYER)
                .isCreator(false)
                .kicked(false)
                .build();
    }

    @Test
    @DisplayName("Создатель кампании может исключить участника")
    void kickMember_byCreator_succeeds() {
        KickMemberRequest request = KickMemberRequest.builder().userId(targetId).build();

        PlayerCharacter pc = PlayerCharacter.builder()
                .id(UUID.randomUUID())
                .name("Hero")
                .owner(targetUser)
                .campaign(campaign)
                .status(CharacterStatus.ACTIVE)
                .build();

        // findCampaign
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        // getUser (creator)
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creatorUser));
        // enforceCreatorOrAdmin -> findByCampaignIdAndUserId for creator
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, creatorId))
                .thenReturn(Optional.of(creatorMember));
        // find target member
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, targetId))
                .thenReturn(Optional.of(targetMember));
        // save kicked member
        when(campaignMemberRepository.save(targetMember)).thenReturn(targetMember);
        // save campaign with new invite code
        when(campaignRepository.save(campaign)).thenReturn(campaign);
        // find characters for kicked player
        when(playerCharacterRepository.findByCampaignIdAndOwnerId(campaignId, targetId))
                .thenReturn(List.of(pc));

        campaignService.kickMember(campaignId, request, "creator");

        assertThat(targetMember.getKicked()).isTrue();
        assertThat(campaign.getInviteCode()).isNotEqualTo("OLD_CODE");
        assertThat(pc.getStatus()).isEqualTo(CharacterStatus.RESERVE);
        verify(campaignMemberRepository).save(targetMember);
        verify(campaignRepository).save(campaign);
        verify(playerCharacterRepository).saveAll(List.of(pc));
    }

    @Test
    @DisplayName("Не-создатель не может исключать участников")
    void kickMember_byNonCreator_throws() {
        User nonCreatorGm = User.builder()
                .id(UUID.randomUUID())
                .username("otherGm")
                .role(Role.GAME_MASTER)
                .build();

        CampaignMember nonCreatorMember = CampaignMember.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .user(nonCreatorGm)
                .roleInCampaign(CampaignRole.GM)
                .isCreator(false)
                .kicked(false)
                .build();

        KickMemberRequest request = KickMemberRequest.builder().userId(targetId).build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("otherGm")).thenReturn(Optional.of(nonCreatorGm));
        // enforceCreatorOrAdmin: non-creator GM found but isCreator=false -> throws
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, nonCreatorGm.getId()))
                .thenReturn(Optional.of(nonCreatorMember));

        assertThatThrownBy(() -> campaignService.kickMember(campaignId, request, "otherGm"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the campaign creator can perform this action");
    }

    @Test
    @DisplayName("Создатель может изменить статус кампании")
    void changeCampaignStatus_byCreator_succeeds() {
        ChangeCampaignStatusRequest request = ChangeCampaignStatusRequest.builder()
                .status("PAUSED")
                .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creatorUser));
        // enforceCreatorOrAdmin
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, creatorId))
                .thenReturn(Optional.of(creatorMember));
        when(campaignRepository.save(campaign)).thenReturn(campaign);
        // toCampaignResponse -> shouldShowInviteCode -> getMembership -> findByCampaignIdAndUserId (already mocked above)
        // toCampaignResponse -> findByCampaignIdAndKickedFalse
        when(campaignMemberRepository.findByCampaignIdAndKickedFalse(campaignId))
                .thenReturn(List.of(creatorMember));

        CampaignResponse response = campaignService.changeCampaignStatus(campaignId, request, "creator");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PAUSED);
        assertThat(response.getStatus()).isEqualTo("PAUSED");
        verify(campaignRepository).save(campaign);
    }

    @Test
    @DisplayName("Не-создатель (GM) не может менять статус кампании")
    void changeCampaignStatus_byNonCreatorGm_throws() {
        User nonCreatorGm = User.builder()
                .id(UUID.randomUUID())
                .username("otherGm")
                .role(Role.GAME_MASTER)
                .build();

        CampaignMember nonCreatorMember = CampaignMember.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .user(nonCreatorGm)
                .roleInCampaign(CampaignRole.GM)
                .isCreator(false)
                .kicked(false)
                .build();

        ChangeCampaignStatusRequest request = ChangeCampaignStatusRequest.builder()
                .status("PAUSED")
                .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("otherGm")).thenReturn(Optional.of(nonCreatorGm));
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, nonCreatorGm.getId()))
                .thenReturn(Optional.of(nonCreatorMember));

        assertThatThrownBy(() -> campaignService.changeCampaignStatus(campaignId, request, "otherGm"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the campaign creator can perform this action");
    }

    @Test
    @DisplayName("При выходе из кампании персонажи игрока переводятся в резерв")
    void leaveCampaign_playerCharactersSetToReserve() {
        PlayerCharacter pc1 = PlayerCharacter.builder()
                .id(UUID.randomUUID())
                .name("Warrior")
                .owner(targetUser)
                .campaign(campaign)
                .status(CharacterStatus.ACTIVE)
                .build();

        PlayerCharacter pc2 = PlayerCharacter.builder()
                .id(UUID.randomUUID())
                .name("Mage")
                .owner(targetUser)
                .campaign(campaign)
                .status(CharacterStatus.ACTIVE)
                .build();

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, targetId))
                .thenReturn(Optional.of(targetMember));
        when(playerCharacterRepository.findByCampaignIdAndOwnerId(campaignId, targetId))
                .thenReturn(List.of(pc1, pc2));

        campaignService.leaveCampaign(campaignId, "target");

        assertThat(pc1.getStatus()).isEqualTo(CharacterStatus.RESERVE);
        assertThat(pc2.getStatus()).isEqualTo(CharacterStatus.RESERVE);
        verify(playerCharacterRepository).saveAll(List.of(pc1, pc2));
        verify(campaignMemberRepository).delete(targetMember);
    }

    @Test
    @DisplayName("Создатель не может покинуть свою кампанию")
    void leaveCampaign_creatorCannotLeave() {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creatorUser));
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, creatorId))
                .thenReturn(Optional.of(creatorMember));

        assertThatThrownBy(() -> campaignService.leaveCampaign(campaignId, "creator"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Campaign creator cannot leave");
    }

    @Test
    @DisplayName("Код приглашения скрыт от игрока, когда в кампании есть GM")
    void inviteCode_hiddenFromPlayerWhenGmExists() {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        // enforceMembershipOrAdmin -> isMemberOfCampaign
        when(campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, targetId))
                .thenReturn(true);
        // getCampaignDetail -> findByCampaignIdAndKickedFalse (for member list)
        when(campaignMemberRepository.findByCampaignIdAndKickedFalse(campaignId))
                .thenReturn(List.of(creatorMember, targetMember));
        // shouldShowInviteCode -> getMembership -> findByCampaignIdAndUserId
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, targetId))
                .thenReturn(Optional.of(targetMember));
        // shouldShowInviteCode -> player checks gmCount; 1 GM exists
        when(campaignMemberRepository.countByCampaignIdAndRoleInCampaignAndKickedFalse(campaignId, CampaignRole.GM))
                .thenReturn(1L);

        CampaignDetailResponse response = campaignService.getCampaignDetail(campaignId, "target");

        assertThat(response.getInviteCode()).isNull();
    }

    @Test
    @DisplayName("Код приглашения виден игроку, когда GM отсутствуют")
    void inviteCode_visibleToPlayerWhenNoGms() {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        // enforceMembershipOrAdmin
        when(campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, targetId))
                .thenReturn(true);
        // getCampaignDetail -> findByCampaignIdAndKickedFalse
        when(campaignMemberRepository.findByCampaignIdAndKickedFalse(campaignId))
                .thenReturn(List.of(targetMember));
        // shouldShowInviteCode -> getMembership
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, targetId))
                .thenReturn(Optional.of(targetMember));
        // shouldShowInviteCode -> player checks gmCount; 0 GMs
        when(campaignMemberRepository.countByCampaignIdAndRoleInCampaignAndKickedFalse(campaignId, CampaignRole.GM))
                .thenReturn(0L);

        CampaignDetailResponse response = campaignService.getCampaignDetail(campaignId, "target");

        assertThat(response.getInviteCode()).isEqualTo("OLD_CODE");
    }

    @Test
    @DisplayName("Игрок не может действовать в приостановленной кампании")
    void enforceCampaignActiveForPlayer_pausedCampaign_throws() {
        Campaign pausedCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name("Paused Campaign")
                .status(CampaignStatus.PAUSED)
                .inviteCode("PAUSED1")
                .build();

        User player = User.builder()
                .id(UUID.randomUUID())
                .username("player1")
                .role(Role.PLAYER)
                .build();

        assertThatThrownBy(() -> campaignService.enforceCampaignActiveForPlayer(pausedCampaign, player))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("read-only for players");
    }

    @Test
    @DisplayName("Исключённый пользователь не может повторно вступить в кампанию")
    void joinCampaign_kickedUserCannotRejoin() {
        User kickedUser = User.builder()
                .id(UUID.randomUUID())
                .username("kickedPlayer")
                .role(Role.PLAYER)
                .build();

        CampaignMember kickedMember = CampaignMember.builder()
                .id(UUID.randomUUID())
                .campaign(campaign)
                .user(kickedUser)
                .roleInCampaign(CampaignRole.PLAYER)
                .isCreator(false)
                .kicked(true)
                .build();

        JoinCampaignRequest request = JoinCampaignRequest.builder()
                .inviteCode("OLD_CODE")
                .build();

        when(userRepository.findByUsername("kickedPlayer")).thenReturn(Optional.of(kickedUser));
        when(campaignRepository.findByInviteCode("OLD_CODE")).thenReturn(Optional.of(campaign));
        // existsByCampaignIdAndUserIdAndKickedFalse -> false (because kicked)
        when(campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, kickedUser.getId()))
                .thenReturn(false);
        // findByCampaignIdAndUserId returns kicked member
        when(campaignMemberRepository.findByCampaignIdAndUserId(campaignId, kickedUser.getId()))
                .thenReturn(Optional.of(kickedMember));

        assertThatThrownBy(() -> campaignService.joinCampaign(request, "kickedPlayer"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You have been kicked from this campaign");
    }
}
