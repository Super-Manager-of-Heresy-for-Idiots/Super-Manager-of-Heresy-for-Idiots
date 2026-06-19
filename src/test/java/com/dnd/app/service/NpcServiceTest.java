package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.NpcSourceType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateNpcRequest;
import com.dnd.app.dto.response.NpcResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.dnd.app.repository.MonsterRepository;
import com.dnd.app.repository.NpcNoteRepository;
import com.dnd.app.repository.SpellRepository;
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
import java.util.Set;
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
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private SpeciesRepository speciesRepository;
    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private MonsterRepository monsterRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;

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

    // --- Source-based creation ---

    private Species buildRace() {
        return Species.builder().id(UUID.randomUUID()).nameEn("Human").build();
    }

    private ContentCharacterClass buildClass() {
        return ContentCharacterClass.builder().id(UUID.randomUUID()).nameRu("Wizard").build();
    }

    private Monster buildCampaignMonster(Campaign campaign) {
        return Monster.builder().id(UUID.randomUUID()).nameRusloc("Гоблин").campaign(campaign).build();
    }

    private Monster buildHomebrewMonster(HomebrewPackage pkg) {
        return Monster.builder().id(UUID.randomUUID()).nameRusloc("Самопал").homebrew(pkg).build();
    }

    private void stubGmCreate(User gm, Campaign campaign) {
        when(userRepository.findByUsername(gm.getUsername())).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaign.getId())).thenReturn(campaign);
        when(npcRepository.save(any(CampaignNpc.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Class-based NPC: абилки и заклинания опциональны, прогрессия не валидируется")
    void createNpc_classBased_abilitiesAndSpellsOptional() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        Species race = buildRace();
        ContentCharacterClass clazz = buildClass();

        stubGmCreate(gm, campaign);
        when(speciesRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(classRepository.findById(clazz.getId())).thenReturn(Optional.of(clazz));

        // Level-20 NPC with no spells and no abilities — must be accepted as-is.
        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Archmage")
                .sourceType(NpcSourceType.CLASS_BASED)
                .raceId(race.getId())
                .classId(clazz.getId())
                .level(20)
                .build();

        NpcResponse response = npcService.createNpc(campaign.getId(), request, gm.getUsername());

        assertEquals(NpcSourceType.CLASS_BASED, response.getSourceType());
        assertEquals(20, response.getLevel());
        assertNull(response.getSpells());
        assertNull(response.getAbilities());
        verifyNoInteractions(spellRepository);
    }

    @Test
    @DisplayName("Class-based NPC: без обязательных полей (race/class/level) — ошибка")
    void createNpc_classBased_missingRequiredFields() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();

        when(userRepository.findByUsername(gm.getUsername())).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaign.getId())).thenReturn(campaign);

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Incomplete")
                .sourceType(NpcSourceType.CLASS_BASED)
                .build();

        assertThrows(BadRequestException.class,
                () -> npcService.createNpc(campaign.getId(), request, gm.getUsername()));
        verify(npcRepository, never()).save(any());
    }

    @Test
    @DisplayName("Monster-based NPC: монстр из ЭТОЙ кампании принимается")
    void createNpc_monsterBased_acceptsSameCampaignMonster() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        Monster monster = buildCampaignMonster(campaign);

        stubGmCreate(gm, campaign);
        when(monsterRepository.findById(monster.getId())).thenReturn(Optional.of(monster));

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Goblin boss")
                .sourceType(NpcSourceType.MONSTER_BASED)
                .sourceMonsterId(monster.getId())
                .build();

        NpcResponse response = npcService.createNpc(campaign.getId(), request, gm.getUsername());

        assertEquals(NpcSourceType.MONSTER_BASED, response.getSourceType());
        assertNotNull(response.getSourceMonster());
        assertEquals(monster.getId(), response.getSourceMonster().getId());
    }

    @Test
    @DisplayName("Monster-based NPC: монстр из ДРУГОЙ кампании отклоняется")
    void createNpc_monsterBased_rejectsForeignCampaignMonster() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        Campaign otherCampaign = buildCampaign();
        Monster foreign = buildCampaignMonster(otherCampaign);

        when(userRepository.findByUsername(gm.getUsername())).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaign.getId())).thenReturn(campaign);
        when(monsterRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Stolen monster")
                .sourceType(NpcSourceType.MONSTER_BASED)
                .sourceMonsterId(foreign.getId())
                .build();

        assertThrows(BadRequestException.class,
                () -> npcService.createNpc(campaign.getId(), request, gm.getUsername()));
        verify(npcRepository, never()).save(any());
    }

    @Test
    @DisplayName("Monster-based NPC: homebrew-монстр подключённого пакета принимается")
    void createNpc_monsterBased_acceptsConnectedHomebrewMonster() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        HomebrewPackage pkg = HomebrewPackage.builder().id(UUID.randomUUID()).build();
        Monster monster = buildHomebrewMonster(pkg);

        stubGmCreate(gm, campaign);
        when(monsterRepository.findById(monster.getId())).thenReturn(Optional.of(monster));
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaign.getId()))
                .thenReturn(Set.of(pkg.getId()));

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Homebrew creature")
                .sourceType(NpcSourceType.MONSTER_BASED)
                .sourceMonsterId(monster.getId())
                .build();

        NpcResponse response = npcService.createNpc(campaign.getId(), request, gm.getUsername());

        assertEquals(monster.getId(), response.getSourceMonster().getId());
    }

    @Test
    @DisplayName("Monster-based NPC: homebrew-монстр неподключённого пакета отклоняется")
    void createNpc_monsterBased_rejectsUnconnectedHomebrewMonster() {
        User gm = buildGm();
        Campaign campaign = buildCampaign();
        HomebrewPackage pkg = HomebrewPackage.builder().id(UUID.randomUUID()).build();
        Monster monster = buildHomebrewMonster(pkg);

        when(userRepository.findByUsername(gm.getUsername())).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaign.getId())).thenReturn(campaign);
        when(monsterRepository.findById(monster.getId())).thenReturn(Optional.of(monster));
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaign.getId()))
                .thenReturn(Set.of());

        CreateNpcRequest request = CreateNpcRequest.builder()
                .name("Unavailable homebrew")
                .sourceType(NpcSourceType.MONSTER_BASED)
                .sourceMonsterId(monster.getId())
                .build();

        assertThrows(BadRequestException.class,
                () -> npcService.createNpc(campaign.getId(), request, gm.getUsername()));
        verify(npcRepository, never()).save(any());
    }
}
