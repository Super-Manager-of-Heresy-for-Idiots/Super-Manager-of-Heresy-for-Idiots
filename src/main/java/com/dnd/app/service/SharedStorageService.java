package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateSharedStorageRequest;
import com.dnd.app.dto.response.ItemInstanceResponse;
import com.dnd.app.dto.response.SharedStorageResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SharedStorageRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedStorageService {

    private final SharedStorageRepository sharedStorageRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;

    @Transactional
    public SharedStorageResponse createStorage(UUID campaignId, CreateSharedStorageRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        SharedStorage storage = SharedStorage.builder()
                .name(request.getName())
                .campaign(campaign)
                .createdBy(user)
                .build();
        storage = sharedStorageRepository.save(storage);

        log.info("Shared storage created: id={}, name='{}', campaignId={}, by={}",
                storage.getId(), storage.getName(), campaignId, username);
        return toResponse(storage);
    }

    @Transactional(readOnly = true)
    public List<SharedStorageResponse> listStorages(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return sharedStorageRepository.findByCampaignId(campaignId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedStorageResponse getStorage(UUID storageId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceMembershipOrAdmin(storage.getCampaign(), user);

        return toResponseWithItems(storage);
    }

    @Transactional
    public void deleteStorage(UUID storageId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceGmOrAdmin(storage.getCampaign(), user);

        sharedStorageRepository.delete(storage);
        log.info("Shared storage deleted: id={}, by={}", storageId, username);
    }

    @Transactional
    public void addItemToStorage(UUID storageId, UUID instanceId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceMembershipOrAdmin(storage.getCampaign(), user);

        ItemInstance instance = itemInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item instance not found"));

        if (instance.getOwnerCharacter() == null) {
            throw new BadRequestException("Item has no owner");
        }
        if (instance.getOwnerCharacter().getCampaign() == null
                || !instance.getOwnerCharacter().getCampaign().getId().equals(storage.getCampaign().getId())) {
            throw new BadRequestException("Item's owner character is not in the same campaign as this storage");
        }
        if (user.getRole() != Role.ADMIN
                && !instance.getOwnerCharacter().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own the character that has this item");
        }

        if (instance.getSlot() != null) {
            throw new BadRequestException("Cannot store an equipped item. Unequip it first.");
        }

        instance.setOwnerCharacter(null);
        instance.setSharedStorage(storage);
        itemInstanceRepository.save(instance);

        log.info("Item added to storage: instanceId={}, storageId={}, by={}", instanceId, storageId, username);
    }

    @Transactional
    public void takeItemFromStorage(UUID storageId, UUID instanceId, UUID characterId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceMembershipOrAdmin(storage.getCampaign(), user);

        ItemInstance instance = itemInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item instance not found"));

        if (instance.getSharedStorage() == null || !instance.getSharedStorage().getId().equals(storageId)) {
            throw new BadRequestException("Item is not in this storage");
        }

        PlayerCharacter character = findCharacter(characterId);

        if (character.getCampaign() == null
                || !character.getCampaign().getId().equals(storage.getCampaign().getId())) {
            throw new BadRequestException("Target character is not in the same campaign as this storage");
        }

        if (user.getRole() != Role.ADMIN
                && !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this character");
        }

        instance.setSharedStorage(null);
        instance.setOwnerCharacter(character);
        itemInstanceRepository.save(instance);

        log.info("Item taken from storage: instanceId={}, storageId={}, toCharacterId={}, by={}",
                instanceId, storageId, characterId, username);
    }

    // --- Private helpers ---

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private SharedStorage findStorage(UUID storageId) {
        return sharedStorageRepository.findById(storageId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared storage not found"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
    }

    private SharedStorageResponse toResponse(SharedStorage storage) {
        return SharedStorageResponse.builder()
                .id(storage.getId())
                .name(storage.getName())
                .campaignId(storage.getCampaign().getId())
                .createdAt(storage.getCreatedAt())
                .build();
    }

    private SharedStorageResponse toResponseWithItems(SharedStorage storage) {
        List<ItemInstanceResponse> items = itemInstanceRepository.findBySharedStorageId(storage.getId()).stream()
                .map(this::toItemResponse)
                .toList();

        return SharedStorageResponse.builder()
                .id(storage.getId())
                .name(storage.getName())
                .campaignId(storage.getCampaign().getId())
                .items(items)
                .createdAt(storage.getCreatedAt())
                .build();
    }

    private ItemInstanceResponse toItemResponse(ItemInstance instance) {
        return ItemInstanceResponse.builder()
                .id(instance.getId())
                .templateId(instance.getTemplate().getId())
                .templateName(instance.getTemplate().getName())
                .displayName(instance.getDisplayName())
                .customName(instance.getCustomName())
                .quantity(instance.getQuantity())
                .isUnique(instance.getIsUnique())
                .slot(instance.getSlot() != null ? instance.getSlot().getCode() : null)
                .notes(instance.getNotes())
                .rarity(instance.getTemplate().getRarity() != null ? instance.getTemplate().getRarity().getCode() : null)
                .build();
    }
}
