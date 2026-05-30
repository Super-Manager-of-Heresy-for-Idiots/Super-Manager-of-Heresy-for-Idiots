package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.AddBagItemRequest;
import com.dnd.app.dto.request.EquipFromBagRequest;
import com.dnd.app.dto.request.UpdateBagSlotRequest;
import com.dnd.app.dto.response.BagSlotResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BagService {

    private final BagSlotRepository bagSlotRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final ArtifactRepository artifactRepository;
    private final InventorySlotRepository inventorySlotRepository;

    @Transactional(readOnly = true)
    public List<BagSlotResponse> listBagContents(UUID characterId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceReadAccess(character, username);

        return bagSlotRepository.findAllByCharacterId(characterId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BagSlotResponse addItem(UUID characterId, AddBagItemRequest request, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceWriteAccess(character, username);

        if (request.getItemTypeId() == null && request.getArtifactId() == null) {
            throw new BadRequestException("Необходимо указать itemTypeId или artifactId");
        }
        if (request.getItemTypeId() != null && request.getArtifactId() != null) {
            throw new BadRequestException("Нельзя указать одновременно itemTypeId и artifactId");
        }

        if (request.getArtifactId() != null) {
            return addArtifact(character, request);
        }
        return addItemType(character, request);
    }

    @Transactional
    public BagSlotResponse updateSlot(UUID characterId, UUID slotId, UpdateBagSlotRequest request, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceWriteAccess(character, username);

        BagSlot slot = bagSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Слот сумки не найден с id: " + slotId));
        if (!slot.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Слот не принадлежит указанному персонажу");
        }

        if (request.getQuantity() != null) {
            if (slot.getArtifact() != null && request.getQuantity() != 1) {
                throw new BadRequestException("Количество артефакта не может отличаться от 1");
            }
            slot.setQuantity(request.getQuantity());
        }
        if (request.getNotes() != null) {
            slot.setNotes(request.getNotes());
        }

        slot = bagSlotRepository.save(slot);
        return toResponse(slot);
    }

    @Transactional
    public void removeItem(UUID characterId, UUID slotId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceWriteAccess(character, username);

        BagSlot slot = bagSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Слот сумки не найден с id: " + slotId));
        if (!slot.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Слот не принадлежит указанному персонажу");
        }

        bagSlotRepository.delete(slot);
        log.info("Удалён предмет из сумки персонажа {}, слот {}", characterId, slotId);
    }

    @Transactional
    public BagSlotResponse equipFromBag(UUID characterId, UUID bagSlotId, EquipFromBagRequest request, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceWriteAccess(character, username);

        BagSlot bagSlot = bagSlotRepository.findById(bagSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Слот сумки не найден с id: " + bagSlotId));
        if (!bagSlot.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Слот не принадлежит указанному персонажу");
        }

        EquipmentSlot targetSlot = parseEquipmentSlot(request.getEquipmentSlot());

        EquipmentSlot requiredSlot = bagSlot.getArtifact() != null
                ? bagSlot.getArtifact().getItemType().getSlot()
                : bagSlot.getItemType().getSlot();
        if (requiredSlot != targetSlot) {
            throw new BadRequestException("Предмет не подходит для слота " + targetSlot
                    + ", требуется " + requiredSlot);
        }

        InventorySlot equipSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, targetSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Слот экипировки не найден"));

        if (equipSlot.getItemType() != null || equipSlot.getArtifact() != null) {
            moveEquipmentToBag(character, equipSlot);
        }

        if (bagSlot.getArtifact() != null) {
            equipSlot.setArtifact(bagSlot.getArtifact());
            equipSlot.setItemType(bagSlot.getArtifact().getItemType());
            bagSlotRepository.delete(bagSlot);
        } else {
            equipSlot.setItemType(bagSlot.getItemType());
            equipSlot.setArtifact(null);
            if (bagSlot.getQuantity() > 1) {
                bagSlot.setQuantity(bagSlot.getQuantity() - 1);
                bagSlotRepository.save(bagSlot);
            } else {
                bagSlotRepository.delete(bagSlot);
            }
        }

        inventorySlotRepository.save(equipSlot);
        log.info("Экипирован предмет из сумки в слот {} персонажа {}", targetSlot, characterId);

        return bagSlot.getId() != null && bagSlot.getQuantity() > 0 && bagSlot.getItemType() != null
                ? toResponse(bagSlot) : null;
    }

    @Transactional
    public BagSlotResponse unequipToBag(UUID characterId, String slotName, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceWriteAccess(character, username);

        EquipmentSlot targetSlot = parseEquipmentSlot(slotName);
        InventorySlot equipSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, targetSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Слот экипировки не найден"));

        if (equipSlot.getItemType() == null && equipSlot.getArtifact() == null) {
            throw new BadRequestException("Слот экипировки пуст");
        }

        BagSlot created = moveEquipmentToBag(character, equipSlot);

        equipSlot.setItemType(null);
        equipSlot.setArtifact(null);
        inventorySlotRepository.save(equipSlot);

        log.info("Снято снаряжение из слота {} в сумку персонажа {}", targetSlot, characterId);
        return toResponse(created);
    }

    private BagSlot moveEquipmentToBag(PlayerCharacter character, InventorySlot equipSlot) {
        if (equipSlot.getArtifact() != null) {
            BagSlot slot = BagSlot.builder()
                    .character(character)
                    .artifact(equipSlot.getArtifact())
                    .quantity(1)
                    .build();
            return bagSlotRepository.save(slot);
        }

        if (equipSlot.getItemType() != null) {
            Optional<BagSlot> existing = bagSlotRepository
                    .findByCharacterIdAndItemTypeId(character.getId(), equipSlot.getItemType().getId());
            if (existing.isPresent()) {
                BagSlot slot = existing.get();
                slot.setQuantity(slot.getQuantity() + 1);
                return bagSlotRepository.save(slot);
            }
            BagSlot slot = BagSlot.builder()
                    .character(character)
                    .itemType(equipSlot.getItemType())
                    .quantity(1)
                    .build();
            return bagSlotRepository.save(slot);
        }

        throw new BadRequestException("Слот экипировки пуст");
    }

    private BagSlotResponse addItemType(PlayerCharacter character, AddBagItemRequest request) {
        ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден с id: " + request.getItemTypeId()));

        Optional<BagSlot> existing = bagSlotRepository
                .findByCharacterIdAndItemTypeId(character.getId(), itemType.getId());
        if (existing.isPresent()) {
            BagSlot slot = existing.get();
            slot.setQuantity(slot.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1));
            if (request.getNotes() != null) {
                slot.setNotes(request.getNotes());
            }
            slot = bagSlotRepository.save(slot);
            return toResponse(slot);
        }

        BagSlot slot = BagSlot.builder()
                .character(character)
                .itemType(itemType)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .notes(request.getNotes())
                .build();
        slot = bagSlotRepository.save(slot);
        log.info("Добавлен предмет {} в сумку персонажа {}", itemType.getName(), character.getId());
        return toResponse(slot);
    }

    private BagSlotResponse addArtifact(PlayerCharacter character, AddBagItemRequest request) {
        Artifact artifact = artifactRepository.findById(request.getArtifactId())
                .orElseThrow(() -> new ResourceNotFoundException("Артефакт не найден с id: " + request.getArtifactId()));

        if (bagSlotRepository.findByCharacterIdAndArtifactId(character.getId(), artifact.getId()).isPresent()) {
            throw new BadRequestException("Этот артефакт уже находится в сумке персонажа");
        }

        BagSlot slot = BagSlot.builder()
                .character(character)
                .artifact(artifact)
                .quantity(1)
                .notes(request.getNotes())
                .build();
        slot = bagSlotRepository.save(slot);
        log.info("Добавлен артефакт {} в сумку персонажа {}", artifact.getName(), character.getId());
        return toResponse(slot);
    }

    private EquipmentSlot parseEquipmentSlot(String slotName) {
        try {
            return EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный слот экипировки: " + slotName);
        }
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден с id: " + characterId));
    }

    private void enforceReadAccess(PlayerCharacter character, String username) {
        User user = findUser(username);
        switch (user.getRole()) {
            case PLAYER -> {
                if (!character.getOwner().getId().equals(user.getId()))
                    throw new AccessDeniedException("У вас нет доступа к этому персонажу");
            }
            case GAME_MASTER -> {
                if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId()))
                    throw new AccessDeniedException("У вас нет доступа к этому персонажу");
            }
            case ADMIN -> { }
        }
    }

    private void enforceWriteAccess(PlayerCharacter character, String username) {
        User user = findUser(username);
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId())) return;
        throw new AccessDeniedException("Только владелец или администратор может управлять сумкой персонажа");
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));
    }

    private BagSlotResponse toResponse(BagSlot slot) {
        BagSlotResponse.BagSlotResponseBuilder b = BagSlotResponse.builder()
                .id(slot.getId())
                .quantity(slot.getQuantity())
                .notes(slot.getNotes())
                .createdAt(slot.getCreatedAt());

        if (slot.getItemType() != null) {
            b.itemTypeId(slot.getItemType().getId());
            b.itemTypeName(slot.getItemType().getName());
            b.itemTypeSlot(slot.getItemType().getSlot().name());
        }
        if (slot.getArtifact() != null) {
            b.artifactId(slot.getArtifact().getId());
            b.artifactName(slot.getArtifact().getName());
            b.artifactRarity(slot.getArtifact().getRarity().name());
        }
        return b.build();
    }
}
