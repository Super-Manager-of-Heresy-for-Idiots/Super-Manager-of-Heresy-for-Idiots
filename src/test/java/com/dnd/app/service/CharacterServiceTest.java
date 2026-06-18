package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.request.UpdateStatRequest;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.mapper.CharacterMapper;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterService: создание и доступ к персонажам")
class CharacterServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private CharacterRaceRepository raceRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private CharacterStatRepository characterStatRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignMemberRepository campaignMemberRepository;
    @Mock private CampaignContentService campaignContentService;
    @Mock private CampaignService campaignService;
    @Mock private CharacterMapper characterMapper;
    @Mock private RaceService raceService;
    @Mock private ReferenceDataService referenceDataService;

    @InjectMocks private CharacterService characterService;

    private User makePlayer(UUID id, String name) {
        return User.builder().id(id).username(name).role(Role.PLAYER).build();
    }

    private User makeGM(UUID id, String name) {
        return User.builder().id(id).username(name).role(Role.GAME_MASTER).build();
    }

    @Test
    @DisplayName("Игрок успешно создаёт персонажа в своей кампании")
    void createCharacter_success() {
        UUID playerId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        Campaign campaign = Campaign.builder().id(campaignId).name("Campaign").build();
        ContentCharacterClass cc = ContentCharacterClass.builder().id(UUID.randomUUID()).nameRu("Fighter").build();
        CharacterRace race = CharacterRace.builder().id(UUID.randomUUID()).name("Human").build();
        CreateCharacterRequest req = CreateCharacterRequest.builder()
                .name("Hero").classId(cc.getId()).raceId(race.getId()).campaignId(campaignId).build();

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, playerId)).thenReturn(true);
        when(campaignContentService.isClassAvailableInCampaign(campaignId, cc.getId())).thenReturn(true);
        when(classRepository.findById(cc.getId())).thenReturn(Optional.of(cc));
        when(raceService.getSelectableRace(campaignId, race.getId())).thenReturn(race);
        when(raceService.buildRaceSnapshotJson(race, null)).thenReturn("{}");
        when(statTypeRepository.findAll()).thenReturn(Collections.emptyList());
        PlayerCharacter saved = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Hero").totalLevel(1)
                .race(race).owner(player).campaign(campaign).build();
        when(characterRepository.saveAndFlush(any(PlayerCharacter.class))).thenReturn(saved);
        when(classLevelRepository.saveAndFlush(any(CharacterClassLevel.class))).thenAnswer(inv -> inv.getArgument(0));
        CharacterResponse expected = CharacterResponse.builder().name("Hero").build();
        when(characterMapper.toResponse(saved)).thenReturn(expected);

        CharacterResponse result = characterService.createCharacter(campaignId, req, "player1");

        assertEquals("Hero", result.getName());
        verify(characterRepository).saveAndFlush(any(PlayerCharacter.class));
        verify(classLevelRepository).saveAndFlush(any(CharacterClassLevel.class));
    }

    @Test
    @DisplayName("Игрок не может получить чужого персонажа")
    void getCharacterById_wrongPlayer_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        User owner = makePlayer(ownerId, "owner");
        User other = makePlayer(otherPlayerId, "other");
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Hero").owner(owner).build();

        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class,
                () -> characterService.getCharacterById(character.getId(), "other"));
    }

    @Test
    @DisplayName("GM может редактировать характеристики участника своей кампании")
    void updateStatValue_gmCanEditCampaignMember() {
        UUID gmId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        UUID statId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        User gm = makeGM(gmId, "gm1");
        User player = makePlayer(playerId, "player1");
        Campaign campaign = Campaign.builder().id(campaignId).name("Campaign").build();
        PlayerCharacter character = PlayerCharacter.builder()
                .id(charId).name("Hero").owner(player).campaign(campaign).build();
        StatType st = StatType.builder().id(UUID.randomUUID()).slug("str").nameRu("STR").nameEn("STR").build();
        CharacterStat stat = CharacterStat.builder()
                .id(statId).character(character).statType(st).value(10).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(campaignService.isGmInCampaign(campaignId, gmId)).thenReturn(true);
        when(characterStatRepository.findById(statId)).thenReturn(Optional.of(stat));
        when(characterStatRepository.save(any())).thenReturn(stat);
        CharacterStatResponse expected = CharacterStatResponse.builder().value(15).build();
        when(characterMapper.toStatResponse(stat)).thenReturn(expected);

        UpdateStatRequest req = UpdateStatRequest.builder().value(15).build();
        CharacterStatResponse result = characterService.updateStatValue(charId, statId, req, "gm1");

        assertEquals(15, result.getValue());
    }

    @Test
    @DisplayName("GM не может редактировать характеристики персонажа вне своей кампании")
    void updateStatValue_gmCannotEditNonCampaignMember() {
        UUID gmId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        User gm = makeGM(gmId, "gm1");
        User player = makePlayer(playerId, "player1");
        Campaign campaign = Campaign.builder().id(campaignId).name("Campaign").build();
        PlayerCharacter character = PlayerCharacter.builder()
                .id(charId).name("Hero").owner(player).campaign(campaign).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(campaignService.isGmInCampaign(campaignId, gmId)).thenReturn(false);

        UpdateStatRequest req = UpdateStatRequest.builder().value(15).build();
        assertThrows(AccessDeniedException.class,
                () -> characterService.updateStatValue(charId, UUID.randomUUID(), req, "gm1"));
    }

    @Test
    @DisplayName("GM не может создавать персонажей-игроков")
    void createCharacter_gameMasterCannotCreate() {
        UUID gmId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        User gm = makeGM(gmId, "gm1");
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));

        CreateCharacterRequest req = CreateCharacterRequest.builder()
                .name("Hero").classId(UUID.randomUUID()).raceId(UUID.randomUUID()).build();

        assertThrows(AccessDeniedException.class,
                () -> characterService.createCharacter(campaignId, req, "gm1"));
    }
}
