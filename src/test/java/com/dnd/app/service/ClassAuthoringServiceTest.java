package com.dnd.app.service;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.grant.CustomTextGrantPayload;
import com.dnd.app.dto.content.grant.SubclassGrantPayload;
import com.dnd.app.dto.request.ClassWriteRequest;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ClassValidationException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantCustomTextRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantNumericModifierRepository;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSubclassRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassAuthoringService: aggregate авторинг класса на новой модели")
class ClassAuthoringServiceTest {

    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private ContentSubclassRepository subclassRepository;
    @Mock private ClassFeatureRepository featureRepository;
    @Mock private ClassLevelRewardGroupRepository groupRepository;
    @Mock private ClassLevelRewardOptionRepository optionRepository;
    @Mock private ClassLevelRewardGrantRepository grantRepository;
    @Mock private ClassLevelRewardGrantFeatureRepository grantFeatureRepository;
    @Mock private ClassLevelRewardGrantSubclassRepository grantSubclassRepository;
    @Mock private ClassLevelRewardGrantFeatRepository grantFeatRepository;
    @Mock private ClassLevelRewardGrantSpellRepository grantSpellRepository;
    @Mock private ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepository;
    @Mock private ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepository;
    @Mock private ClassLevelRewardGrantNumericModifierRepository grantNumericRepository;
    @Mock private ClassLevelRewardGrantCustomTextRepository grantCustomRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private ContentSkillRepository skillRepository;
    @Mock private FeatRepository featRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private HomebrewPackageRepository packageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentClassMapper classMapper;

    @InjectMocks private ClassAuthoringService service;

    private UUID packageId;
    private UUID abilityId;
    private User gm;

    @BeforeEach
    void setUp() {
        packageId = UUID.randomUUID();
        abilityId = UUID.randomUUID();
        gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.GAME_MASTER).build();

        HomebrewPackage pkg = HomebrewPackage.builder().id(packageId).author(gm).build();
        lenient().when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        lenient().when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        lenient().when(classRepository.findAllByHomebrewIdIn(any())).thenReturn(List.of());
        lenient().when(statTypeRepository.findAllById(any()))
                .thenReturn(List.of(StatType.builder().id(abilityId).slug("str").nameRu("Сила").nameEn("Strength").build()));
        lenient().when(classRepository.save(any())).thenAnswer(i -> {
            ContentCharacterClass c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });
        lenient().when(subclassRepository.save(any())).thenAnswer(i -> {
            ContentSubclass s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        lenient().when(groupRepository.save(any())).thenAnswer(i -> {
            ClassLevelRewardGroup g = i.getArgument(0);
            if (g.getId() == null) {
                g.setId(UUID.randomUUID());
            }
            return g;
        });
        lenient().when(optionRepository.save(any())).thenAnswer(i -> {
            ClassLevelRewardOption o = i.getArgument(0);
            if (o.getId() == null) {
                o.setId(UUID.randomUUID());
            }
            return o;
        });
        lenient().when(grantRepository.save(any())).thenAnswer(i -> {
            ClassLevelRewardGrant g = i.getArgument(0);
            if (g.getId() == null) {
                g.setId(UUID.randomUUID());
            }
            return g;
        });
        lenient().when(grantCustomRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(grantSubclassRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(classMapper.toDetail(any(), any()))
                .thenReturn(ContentClassDetailResponse.builder().build());
    }

    private ClassWriteRequest.ClassWriteRequestBuilder baseClass() {
        return ClassWriteRequest.builder()
                .name("Stormbinder").hitDie(8).primaryAbilityIds(List.of(abilityId));
    }

    @Test
    @DisplayName("Создание homebrew-класса с custom-грантом в AUTO-группе")
    void createWithCustomGrant() {
        ClassWriteRequest.GrantInput grant = ClassWriteRequest.GrantInput.builder()
                .grantType("CUSTOM_TEXT").label("Stormborn").sortOrder(0)
                .payload(CustomTextGrantPayload.builder().title("Stormborn").body("Speak with air").build())
                .build();
        ClassWriteRequest.RewardGroupInput group = ClassWriteRequest.RewardGroupInput.builder()
                .classLevel(1).groupKind("AUTO").chooseMin(0).chooseMax(0).grants(List.of(grant)).build();

        var result = service.createPackageClass(packageId, baseClass().rewardGroups(List.of(group)).build(), "gm", "en");

        assertEquals(packageId, result.getPackageId());
        verify(classRepository).save(any());
        verify(grantCustomRepository).save(any());
    }

    @Test
    @DisplayName("Создание класса с выбором подкласса (subclassKey-ссылка в CHOICE-группе)")
    void createWithSubclassChoice() {
        ClassWriteRequest.SubclassInput sub = ClassWriteRequest.SubclassInput.builder()
                .key("sc_thunder").name("Path of Thunder").build();
        ClassWriteRequest.GrantInput subGrant = ClassWriteRequest.GrantInput.builder()
                .grantType("SUBCLASS").sortOrder(0)
                .payload(SubclassGrantPayload.builder().subclassKey("sc_thunder").build())
                .build();
        ClassWriteRequest.RewardOptionInput option = ClassWriteRequest.RewardOptionInput.builder()
                .optionKey("path_of_thunder").label("Path of Thunder").sortOrder(0)
                .grants(List.of(subGrant)).build();
        ClassWriteRequest.RewardGroupInput group = ClassWriteRequest.RewardGroupInput.builder()
                .classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1).options(List.of(option)).build();

        service.createPackageClass(packageId,
                baseClass().subclasses(List.of(sub)).rewardGroups(List.of(group)).build(), "gm", "en");

        verify(subclassRepository).save(any());
        verify(grantSubclassRepository).save(any());
    }

    @Test
    @DisplayName("AUTO-группа с опциями отклоняется валидацией")
    void autoGroupWithOptions_rejected() {
        ClassWriteRequest.RewardOptionInput option = ClassWriteRequest.RewardOptionInput.builder()
                .label("X").grants(List.of()).build();
        ClassWriteRequest.RewardGroupInput group = ClassWriteRequest.RewardGroupInput.builder()
                .classLevel(1).groupKind("AUTO").options(List.of(option)).build();

        ClassWriteRequest req = baseClass().rewardGroups(List.of(group)).build();

        assertThrows(ClassValidationException.class,
                () -> service.createPackageClass(packageId, req, "gm", "en"));
    }

    @Test
    @DisplayName("Недопустимая кость хитов отклоняется валидацией")
    void invalidHitDie_rejected() {
        ClassWriteRequest req = baseClass().hitDie(7).build();
        assertThrows(ClassValidationException.class,
                () -> service.createPackageClass(packageId, req, "gm", "en"));
    }

    @Test
    @DisplayName("Нельзя редактировать чужой homebrew-пакет")
    void foreignPackage_rejected() {
        User other = User.builder().id(UUID.randomUUID()).username("other").role(Role.GAME_MASTER).build();
        HomebrewPackage foreign = HomebrewPackage.builder().id(packageId).author(other).build();
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(foreign));

        ClassWriteRequest req = baseClass().build();
        assertThrows(AccessDeniedException.class,
                () -> service.createPackageClass(packageId, req, "gm", "en"));
    }

    @Test
    @DisplayName("Update заменяет граф: удаляет существующие группы и сохраняет новые")
    void updateReplacesGraph() {
        UUID classId = UUID.randomUUID();
        ContentCharacterClass existing = ContentCharacterClass.builder()
                .id(classId).slug("stormbinder").nameRu("Повелитель бурь").nameEn("Stormbinder")
                .homebrew(HomebrewPackage.builder().id(packageId).author(gm).build()).build();
        when(classRepository.findById(classId)).thenReturn(Optional.of(existing));
        when(groupRepository.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of());
        when(featureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId)).thenReturn(List.of());
        when(subclassRepository.findAllByCharacterClassId(classId)).thenReturn(List.of());

        ClassWriteRequest.RewardGroupInput group = ClassWriteRequest.RewardGroupInput.builder()
                .classLevel(1).groupKind("AUTO").chooseMin(0).chooseMax(0).grants(List.of()).build();

        service.updatePackageClass(packageId, classId,
                baseClass().rewardGroups(List.of(group)).build(), "gm", "en");

        verify(groupRepository).deleteAll(any());
        verify(groupRepository).save(any());
    }
}
