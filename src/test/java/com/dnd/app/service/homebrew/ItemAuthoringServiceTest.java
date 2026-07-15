package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.service.ContentDictionaryResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест ItemAuthoringServiceTest фиксирует авторинг единого homebrew-предмета (P1.5 / IT-2, kind=MAGIC):
 * создание строит magic_item + регистрирует content-item ITEM; EQUIPMENT пока отклоняется.
 */
@ExtendWith(MockitoExtension.class)
class ItemAuthoringServiceTest {

    @Mock private MagicItemRepository magicItemRepository;
    @Mock private HomebrewAccessService homebrewAccessService;
    @Mock private HomebrewContentItemRepository contentItemRepository;
    @Mock private ContentDictionaryResolver contentDictionaryResolver;

    @InjectMocks private ItemAuthoringService service;

    private final UUID packageId = UUID.randomUUID();

    private HomebrewPackage draftPackage() {
        User author = new User();
        author.setId(UUID.randomUUID());
        author.setUsername("gm");
        HomebrewPackage pkg = HomebrewPackage.builder().author(author).title("Pkg").status(HomebrewStatus.DRAFT).build();
        pkg.setId(packageId);
        return pkg;
    }

    @Test
    @DisplayName("create MAGIC: строит magic_item и регистрирует content-item ITEM")
    void createMagic_buildsItemAndContentItem() {
        HomebrewPackage pkg = draftPackage();
        Rarity rarity = new Rarity();
        rarity.setId(UUID.randomUUID());
        rarity.setSlug("legendary");

        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(magicItemRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(contentDictionaryResolver.resolveRarity(eq("legendary"), eq(pkg))).thenReturn(rarity);
        when(magicItemRepository.save(any(MagicItem.class))).thenAnswer(inv -> {
            MagicItem m = inv.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            return m;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(any(), any(), any()))
                .thenReturn(false);

        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setKind("MAGIC");
        req.setName("Посох Луны");
        req.setRarity("legendary");
        req.setAttunementRequired(true);

        HomebrewItemResponse resp = service.create(packageId, req, "gm");

        ArgumentCaptor<MagicItem> captor = ArgumentCaptor.forClass(MagicItem.class);
        verify(magicItemRepository).save(captor.capture());
        MagicItem saved = captor.getValue();
        assertEquals("Посох Луны", saved.getNameRu());
        assertSame(pkg, saved.getHomebrew());
        assertSame(rarity, saved.getRarity());
        assertTrue(saved.getAttunementRequired());
        assertNotNull(saved.getSlug());

        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.ITEM && ci.getContentId().equals(saved.getId())));
        assertEquals("MAGIC", resp.getKind());
        assertEquals("legendary", resp.getRarity());
        assertEquals("HOMEBREW", resp.getSource());
    }

    @Test
    @DisplayName("create EQUIPMENT: пока отклоняется (400)")
    void createEquipment_rejected() {
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(draftPackage());
        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setKind("EQUIPMENT");
        req.setName("Меч");
        assertThrows(BadRequestException.class, () -> service.create(packageId, req, "gm"));
    }
}
