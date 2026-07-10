package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateSharedStorageRequest;
import com.dnd.app.dto.response.ItemInstanceResponse;
import com.dnd.app.dto.response.SharedStorageResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ItemInstanceMapper;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SharedStorageRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Класс SharedStorageService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharedStorageService {

    private final SharedStorageRepository sharedStorageRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;

    /**
     * Создает результат операции "create storage" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list storages" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SharedStorageResponse> listStorages(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return sharedStorageRepository.findByCampaignId(campaignId).stream()
                .map(this::toResponseWithItems)
                .toList();
    }

    /**
     * Возвращает результат операции "get storage" в рамках бизнес-логики домена.
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SharedStorageResponse getStorage(UUID storageId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceMembershipOrAdmin(storage.getCampaign(), user);

        return toResponseWithItems(storage);
    }

    /**
     * Удаляет результат операции "delete storage" в рамках бизнес-логики домена.
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteStorage(UUID storageId, String username) {
        User user = getUser(username);
        SharedStorage storage = findStorage(storageId);
        campaignService.enforceGmOrAdmin(storage.getCampaign(), user);

        sharedStorageRepository.delete(storage);
        log.info("Shared storage deleted: id={}, by={}", storageId, username);
    }

    /**
     * Добавляет результат операции "add item to storage" в рамках бизнес-логики домена.
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param quantity входящее значение quantity, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void addItemToStorage(UUID storageId, UUID instanceId, Integer quantity, String username) {
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
        boolean privileged = user.getRole() == Role.ADMIN
                || campaignService.isGmInCampaign(storage.getCampaign().getId(), user.getId());
        if (!privileged
                && !instance.getOwnerCharacter().getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own the character that has this item");
        }

        if (instance.getSlot() != null) {
            throw new BadRequestException("Cannot store an equipped item. Unequip it first.");
        }

        int moveQty = (quantity != null) ? quantity : instance.getQuantity();
        if (moveQty <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        if (moveQty > instance.getQuantity()) {
            throw new BadRequestException("Not enough items in the stack");
        }

        boolean mergeable = !Boolean.TRUE.equals(instance.getIsUnique());

        if (moveQty < instance.getQuantity()) {
            // Partial deposit: split the source stack, move only moveQty into storage
            instance.setQuantity(instance.getQuantity() - moveQty);
            itemInstanceRepository.save(instance);

            if (mergeable) {
                Optional<ItemInstance> existing = itemInstanceRepository
                        .findStackableForStorage(storageId, tplId(instance), eqId(instance), mgId(instance));
                if (existing.isPresent()) {
                    itemInstanceRepository.incrementQuantity(existing.get().getId(), moveQty);
                    log.info("Item partially deposited (merged): instanceId={}, storageId={}, qty={}, by={}",
                            instanceId, storageId, moveQty, username);
                    return;
                }
            }

            ItemInstance moved = ItemInstance.builder()
                    .template(instance.getTemplate())
                    .equipmentItem(instance.getEquipmentItem())
                    .magicItem(instance.getMagicItem())
                    .sharedStorage(storage)
                    .quantity(moveQty)
                    .customName(instance.getCustomName())
                    .isUnique(instance.getIsUnique())
                    .notes(instance.getNotes())
                    .build();
            itemInstanceRepository.save(moved);
            log.info("Item partially deposited (split): instanceId={}, storageId={}, qty={}, by={}",
                    instanceId, storageId, moveQty, username);
            return;
        }

        // Whole stack: merge into an existing storage stack if possible, else move the instance
        if (mergeable) {
            Optional<ItemInstance> existing = itemInstanceRepository
                    .findStackableForStorage(storageId, tplId(instance), eqId(instance), mgId(instance));
            if (existing.isPresent()) {
                itemInstanceRepository.incrementQuantity(existing.get().getId(), instance.getQuantity());
                itemInstanceRepository.delete(instance);
                log.info("Item deposited (merged whole stack): instanceId={}, storageId={}, qty={}, by={}",
                        instanceId, storageId, instance.getQuantity(), username);
                return;
            }
        }

        instance.setOwnerCharacter(null);
        instance.setSharedStorage(storage);
        itemInstanceRepository.save(instance);

        log.info("Item added to storage: instanceId={}, storageId={}, by={}", instanceId, storageId, username);
    }

    /**
     * Выполняет операции "take item from storage" в рамках бизнес-логики домена.
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param quantity входящее значение quantity, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void takeItemFromStorage(UUID storageId, UUID instanceId, UUID characterId, Integer quantity, String username) {
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

        boolean privileged = user.getRole() == Role.ADMIN
                || campaignService.isGmInCampaign(storage.getCampaign().getId(), user.getId());
        if (!privileged
                && !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not own this character");
        }

        int moveQty = (quantity != null) ? quantity : instance.getQuantity();
        if (moveQty <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        if (moveQty > instance.getQuantity()) {
            throw new BadRequestException("Not enough items in the stack");
        }

        boolean mergeable = !Boolean.TRUE.equals(instance.getIsUnique());

        if (moveQty < instance.getQuantity()) {
            // Partial take: split the storage stack, move only moveQty to the character
            instance.setQuantity(instance.getQuantity() - moveQty);
            itemInstanceRepository.save(instance);

            if (mergeable) {
                Optional<ItemInstance> existing = itemInstanceRepository
                        .findStackableForCharacter(characterId, tplId(instance), eqId(instance), mgId(instance));
                if (existing.isPresent()) {
                    itemInstanceRepository.incrementQuantity(existing.get().getId(), moveQty);
                    log.info("Item partially taken (merged): instanceId={}, storageId={}, toCharacterId={}, qty={}, by={}",
                            instanceId, storageId, characterId, moveQty, username);
                    return;
                }
            }

            ItemInstance moved = ItemInstance.builder()
                    .template(instance.getTemplate())
                    .equipmentItem(instance.getEquipmentItem())
                    .magicItem(instance.getMagicItem())
                    .ownerCharacter(character)
                    .quantity(moveQty)
                    .customName(instance.getCustomName())
                    .isUnique(instance.getIsUnique())
                    .notes(instance.getNotes())
                    .build();
            itemInstanceRepository.save(moved);
            log.info("Item partially taken (split): instanceId={}, storageId={}, toCharacterId={}, qty={}, by={}",
                    instanceId, storageId, characterId, moveQty, username);
            return;
        }

        // Whole stack: merge into the character's existing stack if possible, else move the instance
        if (mergeable) {
            Optional<ItemInstance> existing = itemInstanceRepository
                    .findStackableForCharacter(characterId, tplId(instance), eqId(instance), mgId(instance));
            if (existing.isPresent()) {
                itemInstanceRepository.incrementQuantity(existing.get().getId(), instance.getQuantity());
                itemInstanceRepository.delete(instance);
                log.info("Item taken (merged whole stack): instanceId={}, storageId={}, toCharacterId={}, qty={}, by={}",
                        instanceId, storageId, characterId, instance.getQuantity(), username);
                return;
            }
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
        return ItemInstanceMapper.toResponse(instance);
    }

    private static UUID tplId(ItemInstance i) {
        return i.getTemplate() != null ? i.getTemplate().getId() : null;
    }

    private static UUID eqId(ItemInstance i) {
        return i.getEquipmentItem() != null ? i.getEquipmentItem().getId() : null;
    }

    private static UUID mgId(ItemInstance i) {
        return i.getMagicItem() != null ? i.getMagicItem().getId() : null;
    }
}
