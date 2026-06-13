package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.*;
import com.dnd.app.dto.request.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RaceService: создание и доступ к расам")
class RaceServiceTest {

    @Mock private CharacterRaceRepository raceRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private HomebrewPackageRepository homebrewPackageRepository;
    @Mock private HomebrewContentItemRepository contentItemRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private CampaignService campaignService;
    @Mock private ContentDictionaryResolver contentDictionaryResolver;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private RaceService raceService;

    @Test
    @DisplayName("Админ может создать системную расу")
    void adminCanCreateSystemRace() {
        User admin = user(Role.ADMIN);
        RaceCreateRequest request = baseRequest("Aasimar", "SYSTEM");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(raceRepository.existsByName("Aasimar")).thenReturn(false);
        when(raceRepository.existsBySlug("aasimar")).thenReturn(false);
        when(raceRepository.save(any(CharacterRace.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        var response = raceService.createSystemRace(request, "admin");

        assertEquals("Aasimar", response.getName());
        assertEquals("SYSTEM", response.getSourceType());
        assertFalse(response.getAllowAbilityScoreBonuses());
    }

    @Test
    @DisplayName("Мастер не может создать системную расу")
    void gameMasterCannotCreateSystemRace() {
        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(user(Role.GAME_MASTER)));

        assertThrows(AccessDeniedException.class,
                () -> raceService.createSystemRace(baseRequest("Elf", "SYSTEM"), "gm"));
    }

    @Test
    @DisplayName("Мастер может создать homebrew-расу с бонусами характеристик")
    void gameMasterCanCreateHomebrewRaceWithLegacyBonuses() {
        User gm = user(Role.GAME_MASTER);
        HomebrewPackage pkg = homebrew(gm);
        RaceCreateRequest request = baseRequest("Moon Orc", "HOMEBREW");
        request.setAllowAbilityScoreBonuses(true);
        request.setAbilityScoreBonuses(List.of(RaceAbilityScoreBonusDto.builder()
                .ability("STRENGTH")
                .mode("FIXED")
                .value(2)
                .build()));

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(homebrewPackageRepository.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(raceRepository.existsByName("Moon Orc")).thenReturn(false);
        when(raceRepository.existsBySlug("moon-orc")).thenReturn(false);
        when(raceRepository.save(any(CharacterRace.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        var response = raceService.createHomebrewRace(pkg.getId(), request, "gm");

        assertEquals("HOMEBREW", response.getSourceType());
        assertEquals(pkg.getId(), response.getHomebrewId());
        assertTrue(response.getAllowAbilityScoreBonuses());
        verify(contentItemRepository).save(any(HomebrewContentItem.class));
    }

    @Test
    @DisplayName("Игрок не может создать homebrew-расу")
    void playerCannotCreateHomebrewRace() {
        when(userRepository.findByUsername("player")).thenReturn(Optional.of(user(Role.PLAYER)));

        assertThrows(AccessDeniedException.class,
                () -> raceService.createHomebrewRace(UUID.randomUUID(), baseRequest("Bad", "HOMEBREW"), "player"));
    }

    @Test
    @DisplayName("Игрок видит системные и подключённые homebrew-расы кампании")
    void playerSeesSystemAndAttachedHomebrewRace() {
        User player = user(Role.PLAYER);
        UUID campaignId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        CharacterRace system = CharacterRace.builder().id(UUID.randomUUID()).name("Human").sourceType(RaceSourceType.SYSTEM).active(true).build();
        CharacterRace homebrew = CharacterRace.builder()
                .id(UUID.randomUUID()).name("Moon Orc").sourceType(RaceSourceType.HOMEBREW).active(true)
                .homebrew(HomebrewPackage.builder().id(packageId).title("Pkg").build())
                .build();

        when(userRepository.findByUsername("player")).thenReturn(Optional.of(player));
        when(campaignService.findCampaign(campaignId)).thenReturn(Campaign.builder().id(campaignId).build());
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId)).thenReturn(Set.of(packageId));
        when(raceRepository.findAvailableActive(Set.of(packageId))).thenReturn(List.of(system, homebrew));

        var races = raceService.listAvailableForCampaign(campaignId, "player");

        assertEquals(2, races.size());
    }

    @Test
    @DisplayName("Отключённую расу нельзя выбрать")
    void disabledRaceCannotBeSelected() {
        UUID raceId = UUID.randomUUID();
        CharacterRace race = CharacterRace.builder().id(raceId).name("Disabled").sourceType(RaceSourceType.SYSTEM).active(false).build();
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        assertThrows(BadRequestException.class,
                () -> raceService.getSelectableRace(UUID.randomUUID(), raceId));
    }

    @Test
    @DisplayName("Обязательную родословную нельзя пропустить")
    void requiredLineageCannotBeOmitted() {
        CharacterRace race = CharacterRace.builder()
                .id(UUID.randomUUID())
                .name("Elf")
                .sourceType(RaceSourceType.SYSTEM)
                .active(true)
                .lineageRequired(true)
                .lineagesJson("[{\"id\":\"10000000-0000-0000-0000-000000000201\",\"name\":\"Drow\"}]")
                .build();

        assertThrows(BadRequestException.class,
                () -> raceService.validateLineageSelection(race, null));
    }

    @Test
    @DisplayName("Системная раса PHB 2024 не может иметь бонусы характеристик")
    void phb2024SystemRaceCannotHaveAbilityBonuses() {
        RaceCreateRequest request = baseRequest("Elf", "SYSTEM");
        request.setAllowAbilityScoreBonuses(true);
        request.setAbilityScoreBonuses(List.of(RaceAbilityScoreBonusDto.builder()
                .ability("DEXTERITY")
                .mode("FIXED")
                .value(2)
                .build()));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThrows(BadRequestException.class,
                () -> raceService.createSystemRace(request, "admin"));
    }

    @Test
    @DisplayName("Дублирование системной расы в homebrew создаёт независимую копию")
    void duplicateSystemRaceIntoHomebrewCreatesIndependentCopy() {
        User gm = user(Role.GAME_MASTER);
        HomebrewPackage pkg = homebrew(gm);
        CharacterRace source = CharacterRace.builder()
                .id(UUID.randomUUID())
                .name("Human")
                .description("Base")
                .sourceType(RaceSourceType.SYSTEM)
                .sourceName("Player's Handbook 2024")
                .active(true)
                .creatureType("HUMANOID")
                .sizeOptionsJson("[\"MEDIUM\"]")
                .defaultSize("MEDIUM")
                .speedJson("{\"walk\":30}")
                .allowAbilityScoreBonuses(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(homebrewPackageRepository.findById(pkg.getId())).thenReturn(Optional.of(pkg));
        when(raceRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(raceRepository.existsByName("Human - Package")).thenReturn(false);
        when(raceRepository.save(any(CharacterRace.class))).thenAnswer(inv -> withId(inv.getArgument(0)));

        var response = raceService.duplicateSystemRaceIntoHomebrew(pkg.getId(), source.getId(), "gm");

        assertEquals("HOMEBREW", response.getSourceType());
        assertEquals(pkg.getId(), response.getHomebrewId());
        assertEquals("Human - Package", response.getName());
        verify(contentItemRepository).save(any(HomebrewContentItem.class));
    }

    private RaceCreateRequest baseRequest(String name, String sourceType) {
        return RaceCreateRequest.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Description")
                .sourceType(sourceType)
                .sourceName("Player's Handbook 2024")
                .creatureType("HUMANOID")
                .sizeOptions(List.of("MEDIUM"))
                .defaultSize("MEDIUM")
                .speed(RaceSpeedDto.builder().walk(30).build())
                .traits(List.of(RaceTraitRequest.builder().name("Trait").description("Short").build()))
                .allowAbilityScoreBonuses(false)
                .build();
    }

    private User user(Role role) {
        String username = switch (role) {
            case ADMIN -> "admin";
            case GAME_MASTER -> "gm";
            case PLAYER -> "player";
        };
        return User.builder().id(UUID.randomUUID()).username(username).role(role).build();
    }

    private HomebrewPackage homebrew(User author) {
        return HomebrewPackage.builder()
                .id(UUID.randomUUID())
                .author(author)
                .title("Package")
                .status(HomebrewStatus.DRAFT)
                .build();
    }

    private CharacterRace withId(CharacterRace race) {
        if (race.getId() == null) {
            race.setId(UUID.randomUUID());
        }
        return race;
    }
}
