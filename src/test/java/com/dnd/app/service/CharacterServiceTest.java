package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.request.UpdateStatRequest;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.mapper.CharacterMapper;
import com.dnd.app.repository.*;
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
class CharacterServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterClassRepository classRepository;
    @Mock private CharacterRaceRepository raceRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private CharacterStatRepository characterStatRepository;
    @Mock private InventorySlotRepository inventorySlotRepository;
    @Mock private ItemTypeRepository itemTypeRepository;
    @Mock private CharacterConditionRepository charCondRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamContentService teamContentService;
    @Mock private CharacterMapper characterMapper;

    @InjectMocks private CharacterService characterService;

    private User makePlayer(UUID id, String name) {
        return User.builder().id(id).username(name).role(Role.PLAYER).build();
    }

    private User makeGM(UUID id, String name) {
        return User.builder().id(id).username(name).role(Role.GAME_MASTER).build();
    }

    @Test
    void createCharacter_success() {
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        User gm = makeGM(UUID.randomUUID(), "gm1");
        Team team = Team.builder().id(teamId).name("Party").gameMaster(gm).build();
        CharacterClass cc = CharacterClass.builder().id(UUID.randomUUID()).name("Fighter").build();
        CharacterRace race = CharacterRace.builder().id(UUID.randomUUID()).name("Human").build();
        CreateCharacterRequest req = CreateCharacterRequest.builder()
                .name("Hero").classId(cc.getId()).raceId(race.getId()).teamId(teamId).build();

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByIdTeamIdAndIdPlayerId(teamId, playerId)).thenReturn(true);
        when(classRepository.findById(cc.getId())).thenReturn(Optional.of(cc));
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(statTypeRepository.findAll()).thenReturn(Collections.emptyList());
        PlayerCharacter saved = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Hero").totalLevel(1)
                .race(race).owner(player).team(team).build();
        when(characterRepository.saveAndFlush(any(PlayerCharacter.class))).thenReturn(saved);
        when(classLevelRepository.saveAndFlush(any(CharacterClassLevel.class))).thenAnswer(inv -> inv.getArgument(0));
        CharacterResponse expected = CharacterResponse.builder().name("Hero").build();
        when(characterMapper.toResponse(saved)).thenReturn(expected);

        CharacterResponse result = characterService.createCharacter(req, "player1");

        assertEquals("Hero", result.getName());
        verify(characterRepository).saveAndFlush(any(PlayerCharacter.class));
        verify(classLevelRepository).saveAndFlush(any(CharacterClassLevel.class));
    }

    @Test
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
    void updateStatValue_gmCanEditTeamMember() {
        UUID gmId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        UUID statId = UUID.randomUUID();

        User gm = makeGM(gmId, "gm1");
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = PlayerCharacter.builder()
                .id(charId).name("Hero").owner(player).build();
        StatType st = StatType.builder().id(UUID.randomUUID()).name("STR").build();
        CharacterStat stat = CharacterStat.builder()
                .id(statId).character(character).statType(st).value(10).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(characterRepository.isPlayerInGameMasterTeam(playerId, gmId)).thenReturn(true);
        when(characterStatRepository.findById(statId)).thenReturn(Optional.of(stat));
        when(characterStatRepository.save(any())).thenReturn(stat);
        CharacterStatResponse expected = CharacterStatResponse.builder().value(15).build();
        when(characterMapper.toStatResponse(stat)).thenReturn(expected);

        UpdateStatRequest req = UpdateStatRequest.builder().value(15).build();
        CharacterStatResponse result = characterService.updateStatValue(charId, statId, req, "gm1");

        assertEquals(15, result.getValue());
    }

    @Test
    void updateStatValue_gmCannotEditNonTeamMember() {
        UUID gmId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();

        User gm = makeGM(gmId, "gm1");
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = PlayerCharacter.builder()
                .id(charId).name("Hero").owner(player).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(characterRepository.isPlayerInGameMasterTeam(playerId, gmId)).thenReturn(false);

        UpdateStatRequest req = UpdateStatRequest.builder().value(15).build();
        assertThrows(AccessDeniedException.class,
                () -> characterService.updateStatValue(charId, UUID.randomUUID(), req, "gm1"));
    }

    @Test
    void createCharacter_adminCannotCreate() {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        CreateCharacterRequest req = CreateCharacterRequest.builder()
                .name("Hero").classId(UUID.randomUUID()).raceId(UUID.randomUUID()).build();

        assertThrows(AccessDeniedException.class,
                () -> characterService.createCharacter(req, "admin"));
    }
}
