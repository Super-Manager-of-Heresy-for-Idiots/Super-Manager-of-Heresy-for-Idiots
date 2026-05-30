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
    private final TeamRepository teamRepository;
    private final CharacterMapper characterMapper;

    @Transactional
    public ArtifactResponse createArtifact(CreateArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);

        Team team = null;
        if (gm.getRole() == Role.GAME_MASTER) {
            if (request.getTeamId() == null) {
                throw new BadRequestException("Мастер игры должен указать команду для артефакта");
            }
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
            if (!team.getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Вы не являетесь мастером этой команды");
            }
        } else if (gm.getRole() == Role.ADMIN && request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        }

        ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
        Rarity rarity = Rarity.COMMON;
        if (request.getRarity() != null) {
            try {
                rarity = Rarity.valueOf(request.getRarity());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Некорректная редкость: " + request.getRarity());
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
                .team(team)
                .build();
        artifact = artifactRepository.save(artifact);
        log.info("Artifact created: id={}, name='{}', rarity={}, teamId={}, by gm={}", artifact.getId(), artifact.getName(), artifact.getRarity(), team != null ? team.getId() : "global", username);
        return toResponse(artifact);
    }

    @Transactional(readOnly = true)
    public List<ArtifactResponse> listArtifacts(String username, UUID teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        List<Artifact> artifacts;
        if (user.getRole() == Role.ADMIN) {
            if (teamId != null) {
                artifacts = artifactRepository.findAllByTeamId(teamId);
            } else {
                artifacts = artifactRepository.findAll();
            }
        } else if (user.getRole() == Role.GAME_MASTER) {
            if (teamId != null) {
                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
                if (!team.getGameMaster().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Вы не являетесь мастером этой команды");
                }
                artifacts = artifactRepository.findAllByTeamId(teamId);
            } else {
                artifacts = artifactRepository.findAllByGameMasterId(user.getId());
            }
        } else {
            throw new AccessDeniedException("Игроки не могут просматривать список артефактов");
        }
        return artifacts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(UUID id, String username) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Артефакт не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() == Role.PLAYER) {
            throw new AccessDeniedException("Игроки не могут просматривать детали артефакта");
        }
        if (user.getRole() == Role.GAME_MASTER) {
            if (artifact.getTeam() == null || !artifact.getTeam().getGameMaster().getId().equals(user.getId())) {
                throw new AccessDeniedException("Этот артефакт не принадлежит вашей команде");
            }
        }
        return toResponse(artifact);
    }

    @Transactional
    public ArtifactResponse updateArtifact(UUID id, CreateArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Артефакт не найден"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (artifact.getTeam() == null || !artifact.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Этот артефакт не принадлежит вашей команде");
            }
        }
        ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
        artifact.setName(request.getName());
        artifact.setDescription(request.getDescription());
        artifact.setItemType(itemType);
        if (request.getRarity() != null) {
            try {
                artifact.setRarity(Rarity.valueOf(request.getRarity()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Некорректная редкость: " + request.getRarity());
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
                .orElseThrow(() -> new ResourceNotFoundException("Артефакт не найден"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (artifact.getTeam() == null || !artifact.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Этот артефакт не принадлежит вашей команде");
            }
        }
        log.info("Artifact deleted: id={}, name='{}', by gm={}", id, artifact.getName(), username);
        artifactRepository.delete(artifact);
    }

    @Transactional
    public InventorySlotResponse placeArtifact(UUID characterId, String slotName, PlaceArtifactRequest request, String username) {
        User gm = getGMOrAdmin(username);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (character.getTeam() == null || !character.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
            }
        }
        Artifact artifact = artifactRepository.findById(request.getArtifactId())
                .orElseThrow(() -> new ResourceNotFoundException("Артефакт не найден"));
        if (gm.getRole() == Role.GAME_MASTER && artifact.getTeam() != null
                && !artifact.getTeam().getGameMaster().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Этот артефакт не принадлежит вашей команде");
        }

        EquipmentSlot equipSlot;
        try {
            equipSlot = EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный слот экипировки: " + slotName);
        }

        if (artifact.getItemType().getSlot() != equipSlot) {
            throw new BadRequestException("Несоответствие слота артефакта — артефакту нужен " +
                    artifact.getItemType().getSlot() + ", а выбранный слот: " + equipSlot);
        }

        InventorySlot invSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, equipSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Слот инвентаря не найден"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут управлять артефактами");
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
                .teamId(a.getTeam() != null ? a.getTeam().getId() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
