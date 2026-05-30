package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.AddEnchantmentRequest;
import com.dnd.app.dto.response.EnchantmentResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnchantmentServiceTest {

    @Mock private EnchantmentTypeRepository enchantmentTypeRepository;
    @Mock private InventoryEnchantmentRepository inventoryEnchantmentRepository;
    @Mock private InventorySlotRepository inventorySlotRepository;
    @Mock private ItemEnchantmentRepository itemEnchantmentRepository;
    @Mock private ItemInstanceRepository itemInstanceRepository;
    @Mock private PlayerCharacterRepository playerCharacterRepository;
    @Mock private BuffDebuffRepository buffDebuffRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;

    @InjectMocks private EnchantmentService enchantmentService;

    // ---- helpers ----

    private User buildPlayer(UUID id, String username) {
        return User.builder().id(id).username(username).role(Role.PLAYER).build();
    }

    private User buildGm(UUID id, String username) {
        return User.builder().id(id).username(username).role(Role.GAME_MASTER).build();
    }

    private PlayerCharacter buildCharacter(UUID id, User owner) {
        return PlayerCharacter.builder().id(id).name("Hero").owner(owner)
                .campaign(Campaign.builder().id(UUID.randomUUID()).name("Campaign").build()).build();
    }

    private InventorySlot buildSlot(UUID id, PlayerCharacter pc, ItemType itemType) {
        return InventorySlot.builder().id(id).character(pc).slot(EquipmentSlot.MAIN_HAND).itemType(itemType).build();
    }

    private ItemType buildItemType() {
        return ItemType.builder().id(UUID.randomUUID()).name("Longsword").slot(EquipmentSlot.MAIN_HAND).damageBonus(0).build();
    }

    private EnchantmentType buildEnchantmentType(UUID id) {
        return EnchantmentType.builder().id(id).name("Flame").damageBonus(0).build();
    }

    // ---- Test 1: add enchantment to empty slot -> 409 ----

    @Test
    void addEnchantment_toEmptySlot_throws409() {
        UUID charId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        User player = buildPlayer(UUID.randomUUID(), "player1");
        PlayerCharacter pc = buildCharacter(charId, player);
        InventorySlot slot = buildSlot(slotId, pc, null);

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(playerCharacterRepository.findById(charId)).thenReturn(Optional.of(pc));
        when(inventorySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        AddEnchantmentRequest req = AddEnchantmentRequest.builder().enchantmentTypeId(UUID.randomUUID()).build();
        assertThrows(DuplicateResourceException.class, () -> enchantmentService.addEnchantment(charId, slotId, req, "player1"));
    }

    // ---- Test 2: duplicate enchantment on same slot -> 409 ----

    @Test
    void addEnchantment_duplicateOnSameSlot_throws409() {
        UUID charId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID enchTypeId = UUID.randomUUID();
        User player = buildPlayer(UUID.randomUUID(), "player1");
        PlayerCharacter pc = buildCharacter(charId, player);
        InventorySlot slot = buildSlot(slotId, pc, buildItemType());

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(playerCharacterRepository.findById(charId)).thenReturn(Optional.of(pc));
        when(inventorySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(enchantmentTypeRepository.findById(enchTypeId)).thenReturn(Optional.of(buildEnchantmentType(enchTypeId)));
        when(inventoryEnchantmentRepository.existsByInventorySlotIdAndEnchantmentTypeId(slotId, enchTypeId)).thenReturn(true);

        AddEnchantmentRequest req = AddEnchantmentRequest.builder().enchantmentTypeId(enchTypeId).build();
        assertThrows(DuplicateResourceException.class, () -> enchantmentService.addEnchantment(charId, slotId, req, "player1"));
    }

    // ---- Test 3: add enchantment on other player's character -> 403 ----

    @Test
    void addEnchantment_onOtherPlayersCharacter_throws403() {
        UUID charId = UUID.randomUUID();
        User owner = buildPlayer(UUID.randomUUID(), "owner");
        User other = buildPlayer(UUID.randomUUID(), "other");
        PlayerCharacter pc = buildCharacter(charId, owner);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(playerCharacterRepository.findById(charId)).thenReturn(Optional.of(pc));

        AddEnchantmentRequest req = AddEnchantmentRequest.builder().enchantmentTypeId(UUID.randomUUID()).build();
        assertThrows(AccessDeniedException.class, () -> enchantmentService.addEnchantment(charId, UUID.randomUUID(), req, "other"));
    }

    // ---- Test 4: remove enchantment on other player's character -> 403 ----

    @Test
    void removeEnchantment_onOtherPlayersCharacter_throws403() {
        UUID charId = UUID.randomUUID();
        UUID enchId = UUID.randomUUID();
        User owner = buildPlayer(UUID.randomUUID(), "owner");
        User other = buildPlayer(UUID.randomUUID(), "other");
        PlayerCharacter pc = buildCharacter(charId, owner);
        InventorySlot slot = buildSlot(UUID.randomUUID(), pc, buildItemType());
        InventoryEnchantment enchantment = InventoryEnchantment.builder()
                .id(enchId).inventorySlot(slot).enchantmentType(buildEnchantmentType(UUID.randomUUID())).build();

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(inventoryEnchantmentRepository.findById(enchId)).thenReturn(Optional.of(enchantment));

        assertThrows(AccessDeniedException.class, () -> enchantmentService.removeEnchantment(charId, enchId, "other"));
    }

    // ---- Test 5: getSlotEnchantments by GM of team -> succeeds ----

    @Test
    void getSlotEnchantments_byGmOfTeam_succeeds() {
        UUID charId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        User owner = buildPlayer(UUID.randomUUID(), "owner");
        User gm = buildGm(UUID.randomUUID(), "gm1");
        PlayerCharacter pc = buildCharacter(charId, owner);
        InventorySlot slot = buildSlot(slotId, pc, buildItemType());

        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));
        when(playerCharacterRepository.findById(charId)).thenReturn(Optional.of(pc));
        when(campaignService.isGmInCampaign(pc.getCampaign().getId(), gm.getId())).thenReturn(true);
        when(inventorySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(inventoryEnchantmentRepository.findAllByInventorySlotId(slotId)).thenReturn(List.of());

        List<EnchantmentResponse> result = enchantmentService.getSlotEnchantments(charId, slotId, "gm1");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- Test 6: getSlotEnchantments by GM of other team -> 403 ----

    @Test
    void getSlotEnchantments_byGmOfOtherTeam_throws403() {
        UUID charId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        User owner = buildPlayer(UUID.randomUUID(), "owner");
        User gm = buildGm(UUID.randomUUID(), "gm2");
        PlayerCharacter pc = buildCharacter(charId, owner);

        when(userRepository.findByUsername("gm2")).thenReturn(Optional.of(gm));
        when(playerCharacterRepository.findById(charId)).thenReturn(Optional.of(pc));
        when(campaignService.isGmInCampaign(pc.getCampaign().getId(), gm.getId())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> enchantmentService.getSlotEnchantments(charId, slotId, "gm2"));
    }
}
