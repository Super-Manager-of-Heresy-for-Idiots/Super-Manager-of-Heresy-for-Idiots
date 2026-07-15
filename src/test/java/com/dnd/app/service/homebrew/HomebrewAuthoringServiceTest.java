package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.EquipmentSlot;
import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.ItemType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.service.ContentDictionaryResolver;
import com.dnd.app.dto.request.CreateItemTypeRequest;
import com.dnd.app.dto.response.AttachableContentResponse;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.dto.response.HomebrewDetailResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomebrewAuthoringServiceTest {

    @Mock private HomebrewPackageRepository packageRepository;
    @Mock private HomebrewContentItemRepository contentItemRepository;
    @Mock private GmHomebrewLibraryRepository gmLibraryRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagService tagService;
    @Mock private HomebrewContentValidatorRegistry validatorRegistry;
    @Mock private ItemTypeRepository itemTypeRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private FeatRepository featRepository;
    @Mock private BackgroundRepository backgroundRepository;
    @Mock private ContentSkillRepository contentSkillRepository;
    @Mock private CustomResourceTypeRepository customResourceTypeRepository;
    @Mock private ContentCharacterClassRepository contentCharacterClassRepository;
    @Mock private ContentDictionaryResolver contentDictionaryResolver;

    @InjectMocks private HomebrewAuthoringService service;

    @Test
    @DisplayName("Мастер создаёт тип предмета в своём DRAFT-пакете")
    void gameMasterCreatesItemTypeInOwnDraftPackage() {
        UUID gmId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID itemTypeId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.DRAFT);

        EquipmentSlot mainHand = EquipmentSlot.builder().code("MAIN_HAND").build();
        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(itemTypeRepository.existsByName("Scoped Sword")).thenReturn(false);
        when(contentDictionaryResolver.resolveEquipmentSlot("MAIN_HAND", pkg)).thenReturn(mainHand);
        when(contentDictionaryResolver.resolveDamageType("SLASHING", pkg))
                .thenReturn(DamageType.builder().slug("slashing").build());
        when(itemTypeRepository.save(any(ItemType.class))).thenAnswer(invocation -> {
            ItemType itemType = invocation.getArgument(0);
            itemType.setId(itemTypeId);
            return itemType;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                packageId, ContentType.ITEM_TYPE, itemTypeId)).thenReturn(false);
        when(contentItemRepository.countByPackageGroupedByType(packageId)).thenReturn(List.of());
        when(contentItemRepository.findAllByHomebrewPackageId(packageId)).thenReturn(List.of());

        CreateItemTypeRequest request = CreateItemTypeRequest.builder()
                .name("Scoped Sword")
                .description("Package-only item")
                .slot("MAIN_HAND")
                .damageDice("1d8")
                .damageType("SLASHING")
                .build();

        HomebrewDetailResponse response = service.createPackageItemType(packageId, request, "gm");

        assertEquals(packageId, response.getId());

        ArgumentCaptor<ItemType> itemTypeCaptor = ArgumentCaptor.forClass(ItemType.class);
        verify(itemTypeRepository).save(itemTypeCaptor.capture());
        ItemType savedItemType = itemTypeCaptor.getValue();
        assertSame(pkg, savedItemType.getHomebrew());
        assertSame(mainHand, savedItemType.getSlot());

        ArgumentCaptor<HomebrewContentItem> contentItemCaptor = ArgumentCaptor.forClass(HomebrewContentItem.class);
        verify(contentItemRepository).save(contentItemCaptor.capture());
        HomebrewContentItem contentItem = contentItemCaptor.getValue();
        assertSame(pkg, contentItem.getHomebrewPackage());
        assertEquals(ContentType.ITEM_TYPE, contentItem.getContentType());
        assertEquals(itemTypeId, contentItem.getContentId());
    }

    @Test
    @DisplayName("updatePackageSkill: правит умение в своём DRAFT-пакете")
    void updateSkill_editsOwnPackageSkill() {
        UUID gmId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.DRAFT);
        com.dnd.app.domain.Skill skill = com.dnd.app.domain.Skill.builder()
                .id(skillId).name("Старое имя").description("d").homebrew(pkg).build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
        when(skillRepository.existsByName("Новое имя")).thenReturn(false);
        when(skillRepository.save(any(com.dnd.app.domain.Skill.class))).thenAnswer(i -> i.getArgument(0));
        when(contentItemRepository.countByPackageGroupedByType(packageId)).thenReturn(List.of());
        when(contentItemRepository.findAllByHomebrewPackageId(packageId)).thenReturn(List.of());

        com.dnd.app.dto.request.CreateSkillRequest req = com.dnd.app.dto.request.CreateSkillRequest.builder()
                .name("Новое имя").description("новое").skillType("combat").build();

        service.updatePackageSkill(packageId, skillId, req, "gm");

        assertEquals("Новое имя", skill.getName());
        assertEquals("combat", skill.getSkillType());
        assertSame(gm, skill.getUpdatedBy());
    }

    @Test
    @DisplayName("listAttachableContent: контент автора из ДРУГИХ пакетов, исключая текущий")
    void listAttachable_returnsCrossPackageContent() {
        UUID gmId = UUID.randomUUID();
        UUID targetPkgId = UUID.randomUUID();
        UUID otherPkgId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage target = homebrewPackage(targetPkgId, gm, HomebrewStatus.DRAFT);
        HomebrewPackage other = homebrewPackage(otherPkgId, gm, HomebrewStatus.DRAFT);
        other.setTitle("Package A");

        HomebrewContentItem item = HomebrewContentItem.builder()
                .homebrewPackage(other).contentType(ContentType.SKILL).contentId(skillId).build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findByIdAndAuthorId(targetPkgId, gmId)).thenReturn(Optional.of(target));
        when(validatorRegistry.isKnownType("SKILL")).thenReturn(true);
        when(contentItemRepository.findContentIdsByPackageIdsAndType(Set.of(targetPkgId), ContentType.SKILL))
                .thenReturn(Set.of());
        when(contentItemRepository.findAttachableByAuthorAndType(gmId, ContentType.SKILL))
                .thenReturn(List.of(item));
        when(validatorRegistry.summarize("SKILL", skillId))
                .thenReturn(ContentSummaryDto.builder().id(skillId).name("Огненный шар").description("desc").build());

        List<AttachableContentResponse> result = service.listAttachableContent(targetPkgId, "SKILL", "gm");

        assertEquals(1, result.size());
        assertEquals("Огненный шар", result.get(0).getName());
        assertEquals(otherPkgId, result.get(0).getSourcePackageId());
        assertEquals("Package A", result.get(0).getSourcePackageTitle());
    }

    @Test
    @DisplayName("Мастер не может создавать контент в чужом пакете")
    void gameMasterCannotCreateContentInForeignPackage() {
        User gm = user(UUID.randomUUID(), "gm", Role.GAME_MASTER);
        User other = user(UUID.randomUUID(), "other", Role.GAME_MASTER);
        UUID packageId = UUID.randomUUID();
        HomebrewPackage pkg = homebrewPackage(packageId, other, HomebrewStatus.DRAFT);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));

        CreateItemTypeRequest request = CreateItemTypeRequest.builder()
                .name("Foreign Sword")
                .slot("MAIN_HAND")
                .build();

        assertThrows(AccessDeniedException.class,
                () -> service.createPackageItemType(packageId, request, "gm"));
        verify(itemTypeRepository, never()).save(any());
        verify(contentItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Мастер не может создавать контент в архивном пакете (P1-1: DRAFT/PUBLISHED — можно, ARCHIVED — нет)")
    void gameMasterCannotCreateContentInArchivedPackage() {
        User gm = user(UUID.randomUUID(), "gm", Role.GAME_MASTER);
        UUID packageId = UUID.randomUUID();
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.ARCHIVED);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));

        CreateItemTypeRequest request = CreateItemTypeRequest.builder()
                .name("Archived Sword")
                .slot("MAIN_HAND")
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createPackageItemType(packageId, request, "gm"));
        verify(itemTypeRepository, never()).save(any());
        verify(contentItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPackageBackground: строит предысторию + регистрирует content-item BACKGROUND")
    void createBackground_buildsBackgroundAndContentItem() {
        UUID gmId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID bgId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.DRAFT);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(backgroundRepository.existsByNameRu("Странник")).thenReturn(false);
        when(contentSkillRepository.findByNameRuIn(List.of("Атлетика"))).thenReturn(List.of());
        when(backgroundRepository.save(any(com.dnd.app.domain.Background.class))).thenAnswer(i -> {
            com.dnd.app.domain.Background b = i.getArgument(0);
            if (b.getId() == null) b.setId(bgId);
            return b;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                packageId, ContentType.BACKGROUND, bgId)).thenReturn(false);
        when(contentItemRepository.countByPackageGroupedByType(packageId)).thenReturn(List.of());
        when(contentItemRepository.findAllByHomebrewPackageId(packageId)).thenReturn(List.of());

        com.dnd.app.dto.request.CreateBackgroundRequest req = com.dnd.app.dto.request.CreateBackgroundRequest.builder()
                .name("Странник")
                .description("Скиталец")
                .skillProficiencyNames(List.of("Атлетика"))
                .grantedExtras("Один инструмент ремесленника")
                .build();

        service.createPackageBackground(packageId, req, "gm");

        ArgumentCaptor<com.dnd.app.domain.Background> captor =
                ArgumentCaptor.forClass(com.dnd.app.domain.Background.class);
        verify(backgroundRepository).save(captor.capture());
        com.dnd.app.domain.Background saved = captor.getValue();
        assertEquals("Странник", saved.getNameRu());
        assertSame(pkg, saved.getHomebrew());
        assertNotNull(saved.getSlug());
        assertTrue(saved.getDescription().contains("Один инструмент ремесленника"));

        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.BACKGROUND && ci.getContentId().equals(bgId)));
    }

    @Test
    @DisplayName("createPackageCustomResourceType: строит ресурс + валидирует восстановление по формуле")
    void createCustomResource_buildsResourceAndContentItem() {
        UUID gmId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        UUID resId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.DRAFT);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(customResourceTypeRepository.existsByNameIgnoreCaseAndHomebrew_Id("Мана", packageId)).thenReturn(false);
        when(customResourceTypeRepository.save(any(com.dnd.app.domain.CustomResourceType.class))).thenAnswer(i -> {
            com.dnd.app.domain.CustomResourceType r = i.getArgument(0);
            if (r.getId() == null) r.setId(resId);
            return r;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                packageId, ContentType.CUSTOM_RESOURCE, resId)).thenReturn(false);
        when(contentItemRepository.countByPackageGroupedByType(packageId)).thenReturn(List.of());
        when(contentItemRepository.findAllByHomebrewPackageId(packageId)).thenReturn(List.of());

        com.dnd.app.dto.request.CreateCustomResourceTypeRequest req =
                com.dnd.app.dto.request.CreateCustomResourceTypeRequest.builder()
                        .name("Мана")
                        .maxFormula("class_level(\"mage\")")
                        .shortRestRecovery("none")
                        .longRestRecovery("full")
                        .build();

        service.createPackageCustomResourceType(packageId, req, "gm");

        ArgumentCaptor<com.dnd.app.domain.CustomResourceType> captor =
                ArgumentCaptor.forClass(com.dnd.app.domain.CustomResourceType.class);
        verify(customResourceTypeRepository).save(captor.capture());
        com.dnd.app.domain.CustomResourceType saved = captor.getValue();
        assertEquals("Мана", saved.getName());
        assertEquals("full", saved.getLongRestRecovery());
        assertEquals("none", saved.getShortRestRecovery());
        assertSame(pkg, saved.getHomebrew());

        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.CUSTOM_RESOURCE && ci.getContentId().equals(resId)));
    }

    @Test
    @DisplayName("createPackageCustomResourceType: recovery=formula без формулы → 400")
    void createCustomResource_formulaWithoutFormula_rejected() {
        UUID gmId = UUID.randomUUID();
        UUID packageId = UUID.randomUUID();
        User gm = user(gmId, "gm", Role.GAME_MASTER);
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.DRAFT);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));
        when(customResourceTypeRepository.existsByNameIgnoreCaseAndHomebrew_Id("Мана", packageId)).thenReturn(false);

        com.dnd.app.dto.request.CreateCustomResourceTypeRequest req =
                com.dnd.app.dto.request.CreateCustomResourceTypeRequest.builder()
                        .name("Мана")
                        .maxValue(5)
                        .longRestRecovery("formula")
                        .build();

        assertThrows(BadRequestException.class,
                () -> service.createPackageCustomResourceType(packageId, req, "gm"));
        verify(customResourceTypeRepository, never()).save(any());
    }

    private User user(UUID id, String username, Role role) {
        return User.builder()
                .id(id)
                .username(username)
                .role(role)
                .build();
    }

    private HomebrewPackage homebrewPackage(UUID id, User author, HomebrewStatus status) {
        return HomebrewPackage.builder()
                .id(id)
                .author(author)
                .title("Package")
                .description("Description")
                .status(status)
                .build();
    }
}
