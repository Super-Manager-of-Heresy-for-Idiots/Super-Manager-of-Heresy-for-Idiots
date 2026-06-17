package com.dnd.app.service;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CharacterRace;
import com.dnd.app.domain.ProficiencySkill;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.dto.content.ContentCharacterCreationResponse;
import com.dnd.app.dto.content.LevelUpRequest;
import com.dnd.app.dto.content.LevelUpResultResponse;
import com.dnd.app.dto.request.CreateContentCharacterRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CampaignMemberRepository;
import com.dnd.app.repository.CampaignRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.CharacterWalletRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.PlayerCharacter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContentCharacterCreationService: создание персонажа на новой контент-модели")
class ContentCharacterCreationServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentCharacterClassRepository contentClassRepository;
    @Mock private ContentSkillRepository contentSkillRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private CharacterStatRepository characterStatRepository;
    @Mock private BackgroundRepository backgroundRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private CharacterSkillProficiencyRepository skillProficiencyRepository;
    @Mock private CharacterKnownSpellRepository knownSpellRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private CurrencyTypeRepository currencyTypeRepository;
    @Mock private CharacterWalletRepository walletRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignMemberRepository campaignMemberRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private RaceService raceService;
    @Mock private LevelUpCommandService levelUpCommandService;
    @Mock private EntityManager entityManager;

    @InjectMocks private ContentCharacterCreationService service;

    private UUID campaignId;
    private UUID raceId;
    private UUID backgroundId;
    private User owner;
    private List<StatType> sixStats;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        raceId = UUID.randomUUID();
        backgroundId = UUID.randomUUID();
        owner = User.builder().id(UUID.randomUUID()).username("player1").build();

        sixStats = List.of(
                stat("str", "Strength"), stat("dex", "Dexterity"), stat("con", "Constitution"),
                stat("int", "Intelligence"), stat("wis", "Wisdom"), stat("cha", "Charisma"));

        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);

        Campaign campaign = mock(Campaign.class);
        lenient().when(campaign.getId()).thenReturn(campaignId);

        lenient().when(userRepository.findByUsername("player1")).thenReturn(Optional.of(owner));
        lenient().when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        lenient().when(campaignMemberRepository
                .existsByCampaignIdAndUserIdAndKickedFalse(eq(campaignId), eq(owner.getId()))).thenReturn(true);
        lenient().when(raceService.getSelectableRace(eq(campaignId), eq(raceId)))
                .thenReturn(CharacterRace.builder().id(raceId).lineageRequired(false).build());
        lenient().when(raceService.getSelectableVanillaRace(eq(raceId)))
                .thenReturn(CharacterRace.builder().id(raceId).lineageRequired(false).build());
        lenient().when(backgroundRepository.findById(backgroundId)).thenReturn(Optional.of(mock(Background.class)));
        lenient().when(statTypeRepository.findAll()).thenReturn(sixStats);
        lenient().when(contentSkillRepository.existsById(any())).thenReturn(true);
        lenient().when(characterRepository.saveAndFlush(any())).thenAnswer(inv -> {
            PlayerCharacter pc = inv.getArgument(0);
            pc.setId(UUID.randomUUID());
            return pc;
        });
        lenient().when(classLevelRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(characterStatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(skillProficiencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(entityManager.getReference(eq(ProficiencySkill.class), any()))
                .thenReturn(mock(ProficiencySkill.class));
        lenient().when(levelUpCommandService.applyInitialRewardSelections(any(), any(), any(), any()))
                .thenReturn(LevelUpResultResponse.builder().build());
    }

    private StatType stat(String slug, String nameEn) {
        return StatType.builder().id(UUID.randomUUID()).slug(slug).nameRu(nameEn).nameEn(nameEn).build();
    }

    private ContentSkill skill(String slug) {
        return ContentSkill.builder().id(UUID.randomUUID()).slug(slug).nameRu(slug).nameEn(slug).build();
    }

    /** STANDARD_ARRAY entries mapped onto the 6 stat ids. */
    private List<CreateContentCharacterRequest.AbilityScoreEntry> standardArray() {
        int[] values = {15, 14, 13, 12, 10, 8};
        List<CreateContentCharacterRequest.AbilityScoreEntry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(CreateContentCharacterRequest.AbilityScoreEntry.builder()
                    .statId(sixStats.get(i).getId()).baseValue(values[i]).build());
        }
        return entries;
    }

    private CreateContentCharacterRequest.CreateContentCharacterRequestBuilder baseRequest(UUID classId) {
        return CreateContentCharacterRequest.builder()
                .name("Hero").classId(classId).raceId(raceId).backgroundId(backgroundId)
                .level(1).scoreMethod("STANDARD_ARRAY").abilityScores(standardArray());
    }

    @Test
    @DisplayName("Создание воина (не заклинатель) с двумя навыками класса")
    void createFighter_success() {
        ContentSkill athletics = skill("athletics");
        ContentSkill intimidation = skill("intimidation");
        ContentCharacterClass fighter = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("fighter").nameEn("Fighter").nameRu("Воин")
                .hitDie(10).spellcaster(false).skillChoiceCount(2).skillChoiceAny(false)
                .skillOptions(Set.of(athletics, intimidation)).build();
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));

        CreateContentCharacterRequest req = baseRequest(fighter.getId())
                .chosenSkillIds(List.of(athletics.getId(), intimidation.getId())).build();

        ContentCharacterCreationResponse result = service.createCampaignCharacter(campaignId, req, "player1");

        assertEquals(fighter.getId(), result.getClassId());
        assertEquals(1, result.getTotalLevel());
        assertEquals(2, result.getSkillProficiencyIds().size());
        assertEquals(campaignId, result.getCampaignId());
    }

    @Test
    @DisplayName("Навык вне class_skill_option отклоняется")
    void createFighter_invalidSkill_rejected() {
        ContentSkill athletics = skill("athletics");
        ContentCharacterClass fighter = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("fighter").nameEn("Fighter").nameRu("Воин")
                .hitDie(10).spellcaster(false).skillChoiceCount(2).skillChoiceAny(false)
                .skillOptions(Set.of(athletics)).build();
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));

        CreateContentCharacterRequest req = baseRequest(fighter.getId())
                .chosenSkillIds(List.of(athletics.getId(), UUID.randomUUID())).build();

        assertThrows(BadRequestException.class,
                () -> service.createCampaignCharacter(campaignId, req, "player1"));
    }

    @Test
    @DisplayName("Заклинания у не-заклинателя отклоняются")
    void createNonSpellcasterWithSpells_rejected() {
        ContentCharacterClass fighter = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("fighter").nameEn("Fighter").nameRu("Воин")
                .hitDie(10).spellcaster(false).skillChoiceCount(0).skillChoiceAny(false)
                .skillOptions(Set.of()).build();
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));

        CreateContentCharacterRequest req = baseRequest(fighter.getId())
                .spellIds(List.of(UUID.randomUUID())).build();

        assertThrows(BadRequestException.class,
                () -> service.createCampaignCharacter(campaignId, req, "player1"));
    }

    @Test
    @DisplayName("РџРµСЂРІРёС‡РЅС‹Рµ reward selections РїРµСЂРµРґР°СЋС‚СЃСЏ РІ final reward-selection pipeline")
    void createWithInitialRewardSelections_delegatesToFinalRewardPipeline() {
        ContentCharacterClass fighter = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("fighter").nameEn("Fighter").nameRu("Р’РѕРёРЅ")
                .hitDie(10).spellcaster(false).skillChoiceCount(0).skillChoiceAny(false)
                .skillOptions(Set.of()).build();
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));

        LevelUpRequest.GroupSelection selection = LevelUpRequest.GroupSelection.builder()
                .rewardGroupId(UUID.randomUUID())
                .optionIds(List.of(UUID.randomUUID()))
                .build();
        CreateContentCharacterRequest req = baseRequest(fighter.getId())
                .initialRewardSelections(List.of(selection))
                .build();

        service.createCampaignCharacter(campaignId, req, "player1");

        verify(levelUpCommandService).applyInitialRewardSelections(
                any(PlayerCharacter.class),
                eq(fighter),
                eq(req.getInitialRewardSelections()),
                eq("ru"));
    }

    @Test
    @DisplayName("Homebrew-класс из неактивного пакета отклоняется в кампании")
    void createHomebrewClassNotActive_rejected() {
        HomebrewPackage pkg = HomebrewPackage.builder().id(UUID.randomUUID()).build();
        ContentCharacterClass hbClass = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("stormbinder").nameEn("Stormbinder").nameRu("Повелитель бурь")
                .hitDie(8).spellcaster(false).skillChoiceCount(0).homebrew(pkg).build();
        when(contentClassRepository.findById(hbClass.getId())).thenReturn(Optional.of(hbClass));
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId))
                .thenReturn(Set.of(UUID.randomUUID()));

        CreateContentCharacterRequest req = baseRequest(hbClass.getId()).build();

        assertThrows(BadRequestException.class,
                () -> service.createCampaignCharacter(campaignId, req, "player1"));
    }

    @Test
    @DisplayName("Homebrew-класс недоступен для vanilla-шаблона")
    void createVanilla_homebrewClass_rejected() {
        HomebrewPackage pkg = HomebrewPackage.builder().id(UUID.randomUUID()).build();
        ContentCharacterClass hbClass = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("stormbinder").nameEn("Stormbinder").nameRu("Повелитель бурь")
                .hitDie(8).spellcaster(false).homebrew(pkg).build();
        when(contentClassRepository.findById(hbClass.getId())).thenReturn(Optional.of(hbClass));

        CreateContentCharacterRequest req = baseRequest(hbClass.getId()).build();

        assertThrows(BadRequestException.class, () -> service.createVanillaCharacter(req, "player1"));
    }
}
