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
    @Mock private CharacterClassRepository classRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private FeatRepository featRepository;
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
                .thenReturn(DamageType.builder().code("SLASHING").build());
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
    @DisplayName("Мастер не может создавать контент в опубликованном пакете")
    void gameMasterCannotCreateContentInPublishedPackage() {
        User gm = user(UUID.randomUUID(), "gm", Role.GAME_MASTER);
        UUID packageId = UUID.randomUUID();
        HomebrewPackage pkg = homebrewPackage(packageId, gm, HomebrewStatus.PUBLISHED);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(packageRepository.findById(packageId)).thenReturn(Optional.of(pkg));

        CreateItemTypeRequest request = CreateItemTypeRequest.builder()
                .name("Published Sword")
                .slot("MAIN_HAND")
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createPackageItemType(packageId, request, "gm"));
        verify(itemTypeRepository, never()).save(any());
        verify(contentItemRepository, never()).save(any());
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
