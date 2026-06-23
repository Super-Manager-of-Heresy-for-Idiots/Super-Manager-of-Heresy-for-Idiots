package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.EquipItemRequest;
import com.dnd.app.dto.request.GrantItemRequest;
import com.dnd.app.dto.request.RenameItemRequest;
import com.dnd.app.dto.request.TransferItemRequest;
import com.dnd.app.dto.response.ItemInstanceResponse;
import com.dnd.app.mapper.ItemInstanceMapper;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemInstanceService {

    private final ItemInstanceRepository itemInstanceRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final MagicItemRepository magicItemRepository;
    private final ItemTemplateBuffRepository itemTemplateBuffRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final CharacterActiveEffectRepository characterActiveEffectRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CampaignMemberRepository campaignMemberRepository;
    private final WebSocketEventService webSocketEventService;
    private final ContentDictionaryResolver contentDictionaryResolver;

    @Transactional
    public ItemInstanceResponse grantItem(UUID campaignId, UUID characterId,
                                           GrantItemRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        PlayerCharacter character = findCharacter(characterId);
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character not found in this campaign");
        }

        // Resolve the catalog item from the new content model (equipment / magic) or the
        // legacy template table, depending on the request's itemKind.
        String kind = request.getItemKind() != null ? request.getItemKind().trim().toUpperCase() : "EQUIPMENT";
        ItemTemplate template = null;
        EquipmentItem equipmentItem = null;
        MagicItem magicItem = null;
        boolean stackable;
        switch (kind) {
            case "MAGIC" -> {
                magicItem = magicItemRepository.findById(request.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Magic item not found"));
                stackable = false;
            }
            case "TEMPLATE" -> {
                template = itemTemplateRepository.findById(request.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));
                stackable = Boolean.TRUE.equals(template.getIsStackable());
            }
            default -> {
                equipmentItem = equipmentItemRepository.findById(request.getItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Equipment item not found"));
                stackable = isEquipmentStackable(equipmentItem);
            }
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        boolean unique = Boolean.TRUE.equals(request.getIsUnique()) || request.getCustomName() != null;

        // Handle stacking: atomic increment to avoid lost updates under concurrent grants
        if (stackable && !unique) {
            Optional<ItemInstance> existing = itemInstanceRepository.findStackableForCharacter(
                    characterId,
                    template != null ? template.getId() : null,
                    equipmentItem != null ? equipmentItem.getId() : null,
                    magicItem != null ? magicItem.getId() : null);
            if (existing.isPresent()) {
                UUID instanceId = existing.get().getId();
                itemInstanceRepository.incrementQuantity(instanceId, quantity);
                ItemInstance instance = itemInstanceRepository.findById(instanceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Item instance not found"));
                log.info("Item stacked: instanceId={}, characterId={}, quantity={}",
                        instance.getId(), characterId, instance.getQuantity());
                ItemInstanceResponse stackedResponse = toResponse(instance);
                webSocketEventService.sendCampaignEvent(WebSocketEventType.ITEM_GRANTED, campaignId,
                        characterId, stackedResponse, user.getId());
                return stackedResponse;
            }
        }

        ItemInstance instance = ItemInstance.builder()
                .template(template)
                .equipmentItem(equipmentItem)
                .magicItem(magicItem)
                .ownerCharacter(character)
                .quantity(quantity)
                .customName(request.getCustomName())
                .isUnique(unique)
                .build();
        instance = itemInstanceRepository.save(instance);

        log.info("Item granted: instanceId={}, itemId={}, kind={}, characterId={}, by={}",
                instance.getId(), request.getItemId(), kind, characterId, username);
        ItemInstanceResponse response = toResponse(instance);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.ITEM_GRANTED, campaignId,
                characterId, response, user.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getInventory(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterId(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getEquippedItems(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterIdAndSlotIsNotNull(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getBackpackItems(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterIdAndSlotIsNull(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ItemInstanceResponse equipItem(UUID characterId, UUID instanceId,
                                           EquipItemRequest request, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        ItemInstance instance = findInstance(instanceId);
        if (!instance.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Item does not belong to this character");
        }

        EquipmentSlot slot = contentDictionaryResolver.resolveSystemSlot(request.getSlot());

        itemInstanceRepository.findByOwnerCharacterIdAndSlot(characterId, slot)
                .filter(occupant -> !occupant.getId().equals(instanceId))
                .ifPresent(occupant -> {
                    throw new BadRequestException("Slot " + slot.getCode() + " is already occupied by item "
                            + occupant.getDisplayName() + ". Unequip it first.");
                });

        instance.setSlot(slot);
        instance = itemInstanceRepository.save(instance);

        // Auto-apply template buffs as active effects (legacy template-backed items only)
        if (instance.getTemplate() != null) {
            applyTemplateBuffs(character, instance, user);
        }

        log.info("Item equipped: instanceId={}, slot={}, characterId={}", instanceId, slot.getCode(), characterId);
        return toResponse(instance);
    }

    @Transactional
    public ItemInstanceResponse unequipItem(UUID characterId, UUID instanceId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        ItemInstance instance = findInstance(instanceId);
        if (!instance.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Item does not belong to this character");
        }

        // Auto-remove template buffs (legacy template-backed items only)
        if (instance.getTemplate() != null) {
            removeTemplateBuffs(instance);
        }

        instance.setSlot(null);
        instance = itemInstanceRepository.save(instance);

        log.info("Item unequipped: instanceId={}, characterId={}", instanceId, characterId);
        return toResponse(instance);
    }

    @Transactional
    public void removeItem(UUID campaignId, UUID characterId, UUID instanceId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        ItemInstance instance = findInstance(instanceId);
        if (instance.getOwnerCharacter() == null || !instance.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Item does not belong to this character");
        }
        if (instance.getOwnerCharacter().getCampaign() == null
                || !instance.getOwnerCharacter().getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character not found in this campaign");
        }

        // If equipped, unequip first
        if (instance.getSlot() != null) {
            if (instance.getTemplate() != null) {
                removeTemplateBuffs(instance);
            }
            instance.setSlot(null);
        }

        if (instance.getQuantity() > 1) {
            instance.setQuantity(instance.getQuantity() - 1);
            itemInstanceRepository.save(instance);
            log.info("Item quantity decremented: instanceId={}, newQuantity={}", instanceId, instance.getQuantity());
        } else {
            itemInstanceRepository.delete(instance);
            log.info("Item removed: instanceId={}, characterId={}", instanceId, characterId);
        }

        webSocketEventService.sendCampaignEvent(WebSocketEventType.ITEM_REMOVED, campaignId,
                characterId, Map.of("instanceId", instanceId), user.getId());
    }

    @Transactional
    public ItemInstanceResponse transferItem(UUID campaignId, UUID fromCharId, UUID instanceId,
                                              TransferItemRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        PlayerCharacter fromCharacter = findCharacter(fromCharId);
        if (fromCharacter.getCampaign() == null
                || !fromCharacter.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Source character is not in this campaign");
        }

        if (user.getRole() != Role.ADMIN
                && !campaignService.isGmInCampaign(campaignId, user.getId())
                && !fromCharacter.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own the source character");
        }

        ItemInstance instance = findInstance(instanceId);
        if (instance.getOwnerCharacter() == null || !instance.getOwnerCharacter().getId().equals(fromCharId)) {
            throw new BadRequestException("Item does not belong to this character");
        }

        if (instance.getSlot() != null) {
            throw new BadRequestException("Cannot transfer an equipped item. Unequip it first.");
        }

        PlayerCharacter toCharacter = findCharacter(request.getToCharacterId());

        if (toCharacter.getCampaign() == null
                || !toCharacter.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Target character is not in the same campaign");
        }

        instance.setOwnerCharacter(toCharacter);
        instance = itemInstanceRepository.save(instance);

        log.info("Item transferred: instanceId={}, from={}, to={}", instanceId, fromCharId, request.getToCharacterId());
        return toResponse(instance);
    }

    @Transactional
    public ItemInstanceResponse renameItem(UUID characterId, UUID instanceId,
                                            RenameItemRequest request, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        ItemInstance instance = findInstance(instanceId);
        if (instance.getOwnerCharacter() == null || !instance.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Item does not belong to this character");
        }

        boolean renameEntireStack = request.getRenameEntireStack() != null && request.getRenameEntireStack();

        if (instance.getQuantity() > 1 && !renameEntireStack) {
            // Split: reduce original quantity by 1, create new instance with quantity=1
            instance.setQuantity(instance.getQuantity() - 1);
            itemInstanceRepository.save(instance);

            ItemInstance newInstance = ItemInstance.builder()
                    .template(instance.getTemplate())
                    .equipmentItem(instance.getEquipmentItem())
                    .magicItem(instance.getMagicItem())
                    .ownerCharacter(character)
                    .customName(request.getCustomName())
                    .quantity(1)
                    .isUnique(true)
                    .notes(instance.getNotes())
                    .build();
            newInstance = itemInstanceRepository.save(newInstance);

            log.info("Item split and renamed: originalId={}, newId={}, customName='{}'",
                    instanceId, newInstance.getId(), request.getCustomName());
            return toResponse(newInstance);
        } else {
            // Rename the entire stack or single item
            instance.setCustomName(request.getCustomName());
            instance.setIsUnique(true);
            instance = itemInstanceRepository.save(instance);

            log.info("Item renamed: instanceId={}, customName='{}'", instanceId, request.getCustomName());
            return toResponse(instance);
        }
    }

    // --- Private helpers ---

    private void applyTemplateBuffs(PlayerCharacter character, ItemInstance instance, User appliedBy) {
        List<ItemTemplateBuff> templateBuffs = itemTemplateBuffRepository.findByTemplateId(instance.getTemplate().getId());
        for (ItemTemplateBuff tb : templateBuffs) {
            CharacterActiveEffect effect = CharacterActiveEffect.builder()
                    .character(character)
                    .buffDebuff(tb.getBuffDebuff())
                    .appliedBy(appliedBy)
                    .sourceItemInstance(instance)
                    .remainingRounds(null) // permanent while equipped
                    .build();
            characterActiveEffectRepository.save(effect);
        }
    }

    private void removeTemplateBuffs(ItemInstance instance) {
        characterActiveEffectRepository.deleteBySourceItemInstanceId(instance.getId());
    }

    private void enforceViewAccess(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getCampaign() != null) {
            if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("You are not a member of this character's campaign");
            }
        } else {
            if (!character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You cannot view this character's inventory");
            }
        }
    }

    private void enforceOwnerOrGmOrAdmin(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getOwner().getId().equals(user.getId())) return;
        if (character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) return;
        throw new AccessDeniedException("Only the character owner, campaign GM, or ADMIN can perform this action");
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
    }

    private ItemInstance findInstance(UUID instanceId) {
        return itemInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item instance not found"));
    }

    private ItemInstanceResponse toResponse(ItemInstance instance) {
        return ItemInstanceMapper.toResponse(instance);
    }

    /** Equipment is stackable unless it is a weapon or armor (gear/tools/ammo stack). */
    private boolean isEquipmentStackable(EquipmentItem item) {
        String kind = item.getKind() != null ? item.getKind().toLowerCase() : "";
        return !kind.equals("weapon") && !kind.equals("armor");
    }
}
