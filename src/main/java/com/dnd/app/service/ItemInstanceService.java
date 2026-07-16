package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AttuneItemRequest;
import com.dnd.app.dto.request.EquipItemRequest;
import com.dnd.app.dto.request.GrantItemRequest;
import com.dnd.app.dto.request.RenameItemRequest;
import com.dnd.app.dto.request.TransferItemRequest;
import com.dnd.app.dto.response.ItemAbilitySummary;
import com.dnd.app.dto.response.ItemInstanceResponse;
import com.dnd.app.mapper.ItemInstanceMapper;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс ItemInstanceService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemInstanceService {

    public static final int MAX_ATTUNED_ITEMS = 3;

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
    private final ItemAbilityProvisioningService itemAbilityProvisioningService;
    private final ItemAbilityResolver itemAbilityResolver;

    /**
     * Выполняет операции "grant item" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
                itemAbilityProvisioningService.ensureInstanceResources(instance);
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
        itemAbilityProvisioningService.ensureInstanceResources(instance);

        log.info("Item granted: instanceId={}, itemId={}, kind={}, characterId={}, by={}",
                instance.getId(), request.getItemId(), kind, characterId, username);
        ItemInstanceResponse response = toResponse(instance);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.ITEM_GRANTED, campaignId,
                characterId, response, user.getId());
        return response;
    }

    /**
     * Возвращает результат операции "get inventory" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getInventory(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterId(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Возвращает результат операции "get equipped items" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getEquippedItems(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterIdAndSlotIsNotNull(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Возвращает результат операции "get backpack items" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ItemInstanceResponse> getBackpackItems(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return itemInstanceRepository.findByOwnerCharacterIdAndSlotIsNull(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Выполняет операции "equip item" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
        itemAbilityProvisioningService.ensureInstanceResources(instance);

        log.info("Item equipped: instanceId={}, slot={}, characterId={}", instanceId, slot.getCode(), characterId);
        return toResponse(instance);
    }

    /**
     * Выполняет операции "unequip item" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
        itemAbilityProvisioningService.expireInstanceEffects(instance.getId());

        instance.setSlot(null);
        instance = itemInstanceRepository.save(instance);

        log.info("Item unequipped: instanceId={}, characterId={}", instanceId, characterId);
        return toResponse(instance);
    }

    /**
     * Удаляет результат операции "remove item" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
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
            itemAbilityProvisioningService.expireInstanceEffects(instance.getId());
            instance.setSlot(null);
        }
        resetAttunement(instance);
        itemAbilityProvisioningService.expireInstanceEffects(instance.getId());

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

    /**
     * Выполняет операции "transfer item" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param fromCharId идентификатор from char, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

        resetAttunement(instance);
        itemAbilityProvisioningService.expireInstanceEffects(instance.getId());
        instance.setOwnerCharacter(toCharacter);
        instance = itemInstanceRepository.save(instance);
        itemAbilityProvisioningService.ensureInstanceResources(instance);

        log.info("Item transferred: instanceId={}, from={}, to={}", instanceId, fromCharId, request.getToCharacterId());
        return toResponse(instance);
    }

    /**
     * Настраивает предмет на персонажа.
     * @param campaignId id кампании персонажа
     * @param characterId id персонажа-владельца
     * @param instanceId id экземпляра предмета
     * @param request параметры настройки, включая GM override
     * @param username пользователь, выполняющий действие
     * @return обновлённый предмет
     */
    @Transactional
    public ItemInstanceResponse attuneItem(UUID campaignId, UUID characterId, UUID instanceId,
                                           AttuneItemRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        PlayerCharacter character = findCharacter(characterId);
        ensureCharacterInCampaign(character, campaignId);
        enforceOwnerOrGmOrAdmin(character, user);

        ItemInstance instance = findInstance(instanceId);
        ensureOwnedBy(instance, characterId);
        if (!supportsAttunement(instance)) {
            throw new DuplicateResourceException("ATTUNEMENT_NOT_SUPPORTED");
        }
        if (Boolean.TRUE.equals(instance.getAttuned())) {
            return toResponse(instance);
        }

        boolean gmOverride = request != null && Boolean.TRUE.equals(request.getGmOverride());
        if (gmOverride && user.getRole() != Role.ADMIN && !campaignService.isGmInCampaign(campaignId, user.getId())) {
            throw new AccessDeniedException("Only GM or ADMIN can use attunement override");
        }
        long used = itemInstanceRepository.countByOwnerCharacterIdAndAttunedTrue(characterId);
        if (used >= MAX_ATTUNED_ITEMS && !gmOverride) {
            throw new DuplicateResourceException("ATTUNEMENT_LIMIT_REACHED");
        }
        // HB_UX Фаза 5: структурное ограничение настройки (класс/раса) исполняется здесь; GM может форсировать.
        if (!gmOverride) {
            enforceAttunementRestriction(instance, character);
        }

        instance.setAttuned(true);
        instance.setAttunedAt(Instant.now());
        instance = itemInstanceRepository.save(instance);
        itemAbilityProvisioningService.ensureInstanceResources(instance);
        log.info("Item attuned: instanceId={}, characterId={}, gmOverride={}", instanceId, characterId, gmOverride);
        return toResponse(instance);
    }

    /**
     * Снимает настройку предмета и истекает его feature-rules эффекты.
     * @param campaignId id кампании персонажа
     * @param characterId id персонажа-владельца
     * @param instanceId id экземпляра предмета
     * @param username пользователь, выполняющий действие
     * @return обновлённый предмет
     */
    @Transactional
    public ItemInstanceResponse unattuneItem(UUID campaignId, UUID characterId, UUID instanceId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        ensureCharacterInCampaign(character, campaignId);
        enforceOwnerOrGmOrAdmin(character, user);

        ItemInstance instance = findInstance(instanceId);
        ensureOwnedBy(instance, characterId);
        if (!Boolean.TRUE.equals(instance.getAttuned())) {
            return toResponse(instance);
        }
        resetAttunement(instance);
        itemAbilityProvisioningService.expireInstanceEffects(instance.getId());
        instance = itemInstanceRepository.save(instance);
        log.info("Item unattuned: instanceId={}, characterId={}", instanceId, characterId);
        return toResponse(instance);
    }

    /**
     * Выполняет операции "rename item" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
        ItemInstanceResponse response = ItemInstanceMapper.toResponse(instance);
        if (instance.getOwnerCharacter() == null) {
            response.setAbilities(List.of());
            return response;
        }
        Map<UUID, List<ItemAbilitySummary>> abilities =
                itemAbilityResolver.summariesByInstance(instance.getOwnerCharacter());
        response.setAbilities(abilities.getOrDefault(instance.getId(), List.of()));
        return response;
    }

    private void ensureCharacterInCampaign(PlayerCharacter character, UUID campaignId) {
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character not found in this campaign");
        }
    }

    private void ensureOwnedBy(ItemInstance instance, UUID characterId) {
        if (instance.getOwnerCharacter() == null || !instance.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Item does not belong to this character");
        }
    }

    /**
     * Исполняет структурное ограничение настройки предмета (HB_UX Фаза 5): если у magic_item заданы слаги
     * классов/рас, персонаж обязан подходить хотя бы под один из перечисленных вариантов. Свободный текст
     * условия («только для друидов») НЕ проверяется — он флейвор. Ограничения нет → допускаем.
     * @param instance экземпляр настраиваемого предмета
     * @param character персонаж-владелец
     * @throws BadRequestException если персонаж не подходит под ограничение
     */
    private void enforceAttunementRestriction(ItemInstance instance, PlayerCharacter character) {
        MagicItem mi = instance.getMagicItem();
        if (mi == null) {
            return; // у шаблонных предметов структурного ограничения нет
        }
        Set<String> classReq = slugSet(mi.getAttunementClassSlugs());
        Set<String> raceReq = slugSet(mi.getAttunementRaceSlugs());
        if (classReq.isEmpty() && raceReq.isEmpty()) {
            return;
        }
        Set<String> charClasses = character.getClassLevels().stream()
                .map(CharacterClassLevel::getCharacterClass)
                .filter(java.util.Objects::nonNull)
                .map(c -> c.getSlug() == null ? null : c.getSlug().toLowerCase(Locale.ROOT))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        String charRace = (character.getRace() != null && character.getRace().getSlug() != null)
                ? character.getRace().getSlug().toLowerCase(Locale.ROOT) : null;

        boolean classMatch = !classReq.isEmpty() && charClasses.stream().anyMatch(classReq::contains);
        boolean raceMatch = !raceReq.isEmpty() && charRace != null && raceReq.contains(charRace);
        if (!(classMatch || raceMatch)) {
            StringBuilder allowed = new StringBuilder();
            if (!classReq.isEmpty()) {
                allowed.append("классы: ").append(String.join(", ", classReq));
            }
            if (!raceReq.isEmpty()) {
                if (allowed.length() > 0) {
                    allowed.append("; ");
                }
                allowed.append("расы: ").append(String.join(", ", raceReq));
            }
            throw new BadRequestException("Персонаж не подходит под условие настройки предмета (" + allowed + ")");
        }
    }

    /** Разбирает csv-слаги ограничения в множество (trim/lower); пусто → пустое множество. */
    private static Set<String> slugSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean supportsAttunement(ItemInstance instance) {
        if (instance.getMagicItem() != null) {
            return Boolean.TRUE.equals(instance.getMagicItem().getAttunementRequired());
        }
        if (instance.getTemplate() != null) {
            return Boolean.TRUE.equals(instance.getTemplate().getAttunementRequired());
        }
        return false;
    }

    private void resetAttunement(ItemInstance instance) {
        instance.setAttuned(false);
        instance.setAttunedAt(null);
    }

    /** Equipment is stackable unless it is a weapon or armor (gear/tools/ammo stack). */
    private boolean isEquipmentStackable(EquipmentItem item) {
        String kind = item.getKind() != null ? item.getKind().toLowerCase() : "";
        return !kind.equals("weapon") && !kind.equals("armor");
    }
}
