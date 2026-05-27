package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Rarity;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateArtifactRequest;
import com.dnd.app.dto.request.PlaceArtifactRequest;
import com.dnd.app.dto.response.ArtifactResponse;
import com.dnd.app.dto.response.InventorySlotResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CharacterMapper;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final UserRepository userRepository;
    private final PlayerCharacterRepository characterRepository;
    private final InventorySlotRepository inventorySlotRepository;
    private final CharacterMapper characterMapper;

    @Transactional
    public ArtifactResponse createArtifact(CreateArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);
        ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found"));
        Rarity rarity = Rarity.COMMON;
        if (request.getRarity() != null) {
            try {
                rarity = Rarity.valueOf(request.getRarity());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid rarity: " + request.getRarity());
            }
        }
        Artifact artifact = Artifact.builder()
                .name(request.getName())
                .description(request.getDescription())
                .itemType(itemType)
                .rarity(rarity)
                .properties(request.getProperties())
                .specialAbilities(request.getSpecialAbilities())
                .createdBy(gm)
                .build();
        artifact = artifactRepository.save(artifact);
        log.info("Artifact created: id={}, name='{}', rarity={}, by gm={}", artifact.getId(), artifact.getName(), artifact.getRarity(), username);
        return toResponse(artifact);
    }

    @Transactional(readOnly = true)
    public List<ArtifactResponse> listArtifacts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Artifact> artifacts;
        if (user.getRole() == Role.ADMIN) {
            artifacts = artifactRepository.findAll();
        } else if (user.getRole() == Role.GAME_MASTER) {
            artifacts = artifactRepository.findAllByCreatedById(user.getId());
        } else {
            throw new AccessDeniedException("Players cannot list artifacts");
        }
        return artifacts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(UUID id, String username) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.PLAYER) {
            throw new AccessDeniedException("Players cannot view artifact details");
        }
        if (user.getRole() == Role.GAME_MASTER && !artifact.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You did not create this artifact");
        }
        return toResponse(artifact);
    }

    @Transactional
    public ArtifactResponse updateArtifact(UUID id, CreateArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found"));
        if (gm.getRole() != Role.ADMIN && !artifact.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this artifact");
        }
        ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found"));
        artifact.setName(request.getName());
        artifact.setDescription(request.getDescription());
        artifact.setItemType(itemType);
        if (request.getRarity() != null) {
            try {
                artifact.setRarity(Rarity.valueOf(request.getRarity()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid rarity: " + request.getRarity());
            }
        }
        artifact.setProperties(request.getProperties());
        artifact.setSpecialAbilities(request.getSpecialAbilities());
        artifact = artifactRepository.save(artifact);
        return toResponse(artifact);
    }

    @Transactional
    public void deleteArtifact(UUID id, String username) {
        User gm = getGMOrAdmin(username);
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found"));
        if (gm.getRole() != Role.ADMIN && !artifact.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this artifact");
        }
        log.info("Artifact deleted: id={}, name='{}', by gm={}", id, artifact.getName(), username);
        artifactRepository.delete(artifact);
    }

    @Transactional
    public InventorySlotResponse placeArtifact(UUID characterId, String slotName, PlaceArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (gm.getRole() != Role.ADMIN && !characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), gm.getId())) {
            throw new AccessDeniedException("This character's owner is not in any of your teams");
        }
        Artifact artifact = artifactRepository.findById(request.getArtifactId())
                .orElseThrow(() -> new ResourceNotFoundException("Artifact not found"));

        EquipmentSlot equipSlot;
        try {
            equipSlot = EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid equipment slot: " + slotName);
        }

        if (artifact.getItemType().getSlot() != equipSlot) {
            throw new BadRequestException("Artifact slot mismatch — artifact requires " +
                    artifact.getItemType().getSlot() + " but target slot is " + equipSlot);
        }

        InventorySlot invSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, equipSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory slot not found"));
        invSlot.setArtifact(artifact);
        invSlot.setItemType(artifact.getItemType());
        invSlot.setQuantity(1);
        invSlot.setNotes(artifact.getName() + " [" + artifact.getRarity() + "]");
        invSlot = inventorySlotRepository.save(invSlot);
        log.info("Artifact placed: artifact='{}', slot={}, characterId={}, by gm={}",
                artifact.getName(), equipSlot, characterId, username);
        return characterMapper.toInventorySlotResponse(invSlot);
    }

    private User getGMOrAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only game masters can manage artifacts");
        }
        return user;
    }

    private ArtifactResponse toResponse(Artifact a) {
        return ArtifactResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .itemTypeId(a.getItemType().getId())
                .itemTypeName(a.getItemType().getName())
                .itemTypeSlot(a.getItemType().getSlot().name())
                .rarity(a.getRarity().name())
                .properties(a.getProperties())
                .specialAbilities(a.getSpecialAbilities())
                .createdById(a.getCreatedBy().getId())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
