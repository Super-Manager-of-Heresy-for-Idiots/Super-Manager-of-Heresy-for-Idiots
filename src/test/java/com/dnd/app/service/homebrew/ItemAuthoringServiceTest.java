package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.DiceFormula;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.WeaponStat;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.ArmorStatRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.DiceFormulaRepository;
import com.dnd.app.repository.EquipmentCategoryRepository;
import com.dnd.app.repository.EquipmentItemRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.repository.MoneyValueRepository;
import com.dnd.app.repository.WeaponStatRepository;
import com.dnd.app.service.ContentDictionaryResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест ItemAuthoringServiceTest фиксирует авторинг единого homebrew-предмета (P1.5 / IT-2):
 * MAGIC строит magic_item; EQUIPMENT (weapon) строит equipment_item + weapon_stat + dice_formula;
 * оба регистрируют content-item ITEM; TEMPLATE запрещён.
 */
@ExtendWith(MockitoExtension.class)
class ItemAuthoringServiceTest {

    @Mock private MagicItemRepository magicItemRepository;
    @Mock private EquipmentItemRepository equipmentItemRepository;
    @Mock private WeaponStatRepository weaponStatRepository;
    @Mock private ArmorStatRepository armorStatRepository;
    @Mock private DiceFormulaRepository diceFormulaRepository;
    @Mock private MoneyValueRepository moneyValueRepository;
    @Mock private EquipmentCategoryRepository equipmentCategoryRepository;
    @Mock private CurrencyTypeRepository currencyTypeRepository;
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
    @DisplayName("create EQUIPMENT (weapon): строит equipment_item + weapon_stat + dice_formula + content-item")
    void createEquipmentWeapon_buildsEquipmentAndWeaponStat() {
        HomebrewPackage pkg = draftPackage();
        DamageType slashing = new DamageType();
        slashing.setId(UUID.randomUUID());
        slashing.setSlug("slashing");

        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(equipmentItemRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(equipmentItemRepository.save(any(EquipmentItem.class))).thenAnswer(inv -> {
            EquipmentItem e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
        when(armorStatRepository.findById(any())).thenReturn(Optional.empty());
        when(weaponStatRepository.findById(any())).thenReturn(Optional.empty());
        when(diceFormulaRepository.save(any(DiceFormula.class))).thenAnswer(inv -> inv.getArgument(0));
        when(weaponStatRepository.save(any(WeaponStat.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentDictionaryResolver.resolveDamageType(eq("slashing"), eq(pkg))).thenReturn(slashing);
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(any(), any(), any()))
                .thenReturn(false);

        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setKind("EQUIPMENT");
        req.setEquipmentKind("weapon");
        req.setName("Длинный меч авторства");
        req.setDamageDiceCount(1);
        req.setDamageDieSize(8);
        req.setDamageType("slashing");

        HomebrewItemResponse resp = service.create(packageId, req, "gm");

        ArgumentCaptor<EquipmentItem> eqCaptor = ArgumentCaptor.forClass(EquipmentItem.class);
        verify(equipmentItemRepository).save(eqCaptor.capture());
        EquipmentItem savedEquip = eqCaptor.getValue();
        assertEquals("weapon", savedEquip.getKind());
        assertSame(pkg, savedEquip.getHomebrew());
        assertNotNull(savedEquip.getSlug());

        ArgumentCaptor<WeaponStat> wsCaptor = ArgumentCaptor.forClass(WeaponStat.class);
        verify(weaponStatRepository).save(wsCaptor.capture());
        WeaponStat ws = wsCaptor.getValue();
        assertSame(savedEquip, ws.getEquipmentItem());
        assertSame(slashing, ws.getDamageType());
        assertNotNull(ws.getDamageDiceFormula());
        assertEquals(8, ws.getDamageDiceFormula().getDieSize());

        verify(diceFormulaRepository).save(any(DiceFormula.class));
        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.ITEM && ci.getContentId().equals(savedEquip.getId())));
        assertEquals("EQUIPMENT", resp.getKind());
        assertEquals("weapon", resp.getEquipmentKind());
        assertEquals("slashing", resp.getDamageType());
    }

    @Test
    @DisplayName("create EQUIPMENT: неизвестный equipmentKind → 400")
    void createEquipment_invalidKind_rejected() {
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(draftPackage());
        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setKind("EQUIPMENT");
        req.setEquipmentKind("spaceship");
        req.setName("НЛО");
        assertThrows(BadRequestException.class, () -> service.create(packageId, req, "gm"));
    }

    @Test
    @DisplayName("create TEMPLATE: запрещён (400)")
    void createTemplate_rejected() {
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(draftPackage());
        HomebrewItemRequest req = new HomebrewItemRequest();
        req.setKind("TEMPLATE");
        req.setName("Легаси");
        assertThrows(BadRequestException.class, () -> service.create(packageId, req, "gm"));
    }
}
