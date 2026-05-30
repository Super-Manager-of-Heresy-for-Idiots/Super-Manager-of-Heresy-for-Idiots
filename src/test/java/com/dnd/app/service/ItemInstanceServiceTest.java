package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Rarity;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.EquipItemRequest;
import com.dnd.app.dto.request.GrantItemRequest;
import com.dnd.app.dto.request.RenameItemRequest;
import com.dnd.app.dto.request.TransferItemRequest;
import com.dnd.app.dto.response.ItemInstanceResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemInstanceServiceTest {

    @Mock
    private ItemInstanceRepository itemInstanceRepository;

    @Mock
    private ItemTemplateRepository itemTemplateRepository;

    @Mock
    private ItemTemplateBuffRepository itemTemplateBuffRepository;

    @Mock
    private PlayerCharacterRepository playerCharacterRepository;

    @Mock
    private CharacterActiveEffectRepository characterActiveEffectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private CampaignMemberRepository campaignMemberRepository;

    @InjectMocks
    private ItemInstanceService itemInstanceService;

    private static final String USERNAME = "testuser";

    private UUID userId;
    private UUID campaignId;
    private UUID fromCharId;
    private UUID toCharId;
    private UUID instanceId;
    private UUID templateId;

    private User user;
    private Campaign campaign;
    private PlayerCharacter fromCharacter;
    private ItemTemplate template;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
        fromCharId = UUID.randomUUID();
        toCharId = UUID.randomUUID();
        instanceId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username(USERNAME)
                .role(Role.PLAYER)
                .build();

        campaign = Campaign.builder()
                .id(campaignId)
                .name("Test Campaign")
                .build();

        fromCharacter = PlayerCharacter.builder()
                .id(fromCharId)
                .name("FromChar")
                .owner(user)
                .campaign(campaign)
                .build();

        template = ItemTemplate.builder()
                .id(templateId)
                .name("Sword")
                .isStackable(false)
                .rarity(Rarity.COMMON)
                .build();
    }

    // ========================================================================
    // transferItem tests
    // ========================================================================

    @Test
    void transferItem_equippedItem_throws() {
        // Arrange
        ItemInstance equippedInstance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(1)
                .isUnique(false)
                .slot(EquipmentSlot.MAIN_HAND)
                .build();

        TransferItemRequest request = TransferItemRequest.builder()
                .toCharacterId(toCharId)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceMembershipOrAdmin(any(), any());
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(equippedInstance));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                itemInstanceService.transferItem(campaignId, fromCharId, instanceId, request, USERNAME));

        assertTrue(exception.getMessage().contains("Cannot transfer an equipped item"));
    }

    @Test
    void transferItem_differentCampaign_throws() {
        // Arrange
        Campaign otherCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name("Other Campaign")
                .build();

        PlayerCharacter toCharacter = PlayerCharacter.builder()
                .id(toCharId)
                .name("ToChar")
                .owner(user)
                .campaign(otherCampaign)
                .build();

        ItemInstance unequippedInstance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(1)
                .isUnique(false)
                .slot(null)
                .build();

        TransferItemRequest request = TransferItemRequest.builder()
                .toCharacterId(toCharId)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceMembershipOrAdmin(any(), any());
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(unequippedInstance));
        when(playerCharacterRepository.findById(toCharId)).thenReturn(Optional.of(toCharacter));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                itemInstanceService.transferItem(campaignId, fromCharId, instanceId, request, USERNAME));

        assertTrue(exception.getMessage().contains("Target character is not in the same campaign"));
    }

    @Test
    void transferItem_sameCampaign_succeeds() {
        // Arrange
        PlayerCharacter toCharacter = PlayerCharacter.builder()
                .id(toCharId)
                .name("ToChar")
                .owner(user)
                .campaign(campaign)
                .build();

        ItemInstance unequippedInstance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(1)
                .isUnique(false)
                .slot(null)
                .build();

        TransferItemRequest request = TransferItemRequest.builder()
                .toCharacterId(toCharId)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceMembershipOrAdmin(any(), any());
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(unequippedInstance));
        when(playerCharacterRepository.findById(toCharId)).thenReturn(Optional.of(toCharacter));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.transferItem(campaignId, fromCharId, instanceId, request, USERNAME);

        // Assert
        assertNotNull(response);
        verify(itemInstanceRepository).save(any(ItemInstance.class));
    }

    // ========================================================================
    // grantItem tests
    // ========================================================================

    @Test
    void grantItem_stackableExisting_incrementsQuantity() {
        // Arrange
        template.setIsStackable(true);

        ItemInstance existingInstance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(3)
                .isUnique(false)
                .build();

        GrantItemRequest request = GrantItemRequest.builder()
                .templateId(templateId)
                .quantity(2)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceGmOrAdmin(any(), any());
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(itemInstanceRepository.findByOwnerCharacterIdAndTemplateIdAndSlotIsNullAndIsUniqueFalse(fromCharId, templateId))
                .thenReturn(Optional.of(existingInstance));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.grantItem(campaignId, fromCharId, request, USERNAME);

        // Assert
        ArgumentCaptor<ItemInstance> captor = ArgumentCaptor.forClass(ItemInstance.class);
        verify(itemInstanceRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getQuantity());
    }

    @Test
    void grantItem_stackableNoExisting_createsNew() {
        // Arrange
        template.setIsStackable(true);

        GrantItemRequest request = GrantItemRequest.builder()
                .templateId(templateId)
                .quantity(2)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        doNothing().when(campaignService).enforceGmOrAdmin(any(), any());
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(itemInstanceRepository.findByOwnerCharacterIdAndTemplateIdAndSlotIsNullAndIsUniqueFalse(fromCharId, templateId))
                .thenReturn(Optional.empty());
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.grantItem(campaignId, fromCharId, request, USERNAME);

        // Assert
        assertNotNull(response);
        ArgumentCaptor<ItemInstance> captor = ArgumentCaptor.forClass(ItemInstance.class);
        verify(itemInstanceRepository).save(captor.capture());
        ItemInstance savedInstance = captor.getValue();
        assertEquals(2, savedInstance.getQuantity());
        assertEquals(template, savedInstance.getTemplate());
        assertEquals(fromCharacter, savedInstance.getOwnerCharacter());
    }

    // ========================================================================
    // equipItem tests
    // ========================================================================

    @Test
    void equipItem_autoAppliesBuffs() {
        // Arrange
        UUID buffDebuffId = UUID.randomUUID();
        BuffDebuff buffDebuff = BuffDebuff.builder()
                .id(buffDebuffId)
                .name("Strength Buff")
                .isBuff(true)
                .effectType("STAT_MODIFIER")
                .modifierValue(2)
                .build();

        ItemTemplateBuff templateBuff = ItemTemplateBuff.builder()
                .id(UUID.randomUUID())
                .template(template)
                .buffDebuff(buffDebuff)
                .build();

        ItemInstance instance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(1)
                .isUnique(false)
                .slot(null)
                .build();

        EquipItemRequest request = EquipItemRequest.builder()
                .slot("MAIN_HAND")
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemTemplateBuffRepository.findByTemplateId(templateId)).thenReturn(List.of(templateBuff));
        when(characterActiveEffectRepository.save(any(CharacterActiveEffect.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.equipItem(fromCharId, instanceId, request, USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals("MAIN_HAND", response.getSlot());
        verify(characterActiveEffectRepository, times(1)).save(any(CharacterActiveEffect.class));
    }

    // ========================================================================
    // unequipItem tests
    // ========================================================================

    @Test
    void unequipItem_removesBuffs() {
        // Arrange
        UUID buffDebuffId = UUID.randomUUID();
        BuffDebuff buffDebuff = BuffDebuff.builder()
                .id(buffDebuffId)
                .name("Strength Buff")
                .isBuff(true)
                .effectType("STAT_MODIFIER")
                .modifierValue(2)
                .build();

        ItemTemplateBuff templateBuff = ItemTemplateBuff.builder()
                .id(UUID.randomUUID())
                .template(template)
                .buffDebuff(buffDebuff)
                .build();

        ItemInstance instance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(1)
                .isUnique(false)
                .slot(EquipmentSlot.MAIN_HAND)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(itemTemplateBuffRepository.findByTemplateId(templateId)).thenReturn(List.of(templateBuff));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.unequipItem(fromCharId, instanceId, USERNAME);

        // Assert
        assertNotNull(response);
        assertNull(response.getSlot());
        verify(characterActiveEffectRepository, times(1))
                .deleteByCharacterIdAndBuffDebuffId(fromCharId, buffDebuffId);
    }

    // ========================================================================
    // renameItem tests
    // ========================================================================

    @Test
    void renameItem_stackSplit() {
        // Arrange
        ItemInstance instance = ItemInstance.builder()
                .id(instanceId)
                .template(template)
                .ownerCharacter(fromCharacter)
                .quantity(5)
                .isUnique(false)
                .slot(null)
                .build();

        RenameItemRequest request = RenameItemRequest.builder()
                .customName("Excalibur")
                .renameEntireStack(false)
                .build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(fromCharId)).thenReturn(Optional.of(fromCharacter));
        when(itemInstanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ItemInstanceResponse response = itemInstanceService.renameItem(fromCharId, instanceId, request, USERNAME);

        // Assert
        // save should be called twice: once for original (qty-1), once for new (qty=1)
        ArgumentCaptor<ItemInstance> captor = ArgumentCaptor.forClass(ItemInstance.class);
        verify(itemInstanceRepository, times(2)).save(captor.capture());

        List<ItemInstance> savedInstances = captor.getAllValues();

        // First save: original instance with quantity decremented to 4
        ItemInstance originalSaved = savedInstances.get(0);
        assertEquals(4, originalSaved.getQuantity());

        // Second save: new instance with quantity=1, customName="Excalibur", isUnique=true
        ItemInstance newSaved = savedInstances.get(1);
        assertEquals(1, newSaved.getQuantity());
        assertEquals("Excalibur", newSaved.getCustomName());
        assertTrue(newSaved.getIsUnique());

        // The response should represent the new (split) instance
        assertEquals("Excalibur", response.getCustomName());
        assertEquals(1, response.getQuantity());
    }
}
