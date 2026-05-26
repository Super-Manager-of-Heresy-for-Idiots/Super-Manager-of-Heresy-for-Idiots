package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.request.UpdateCharacterRequest;
import com.dnd.app.dto.request.UpdateInventorySlotRequest;
import com.dnd.app.dto.request.UpdateStatRequest;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
import com.dnd.app.dto.response.InventorySlotResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CharacterMapper;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final InventorySlotRepository inventorySlotRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final CharacterMapper characterMapper;

    @Transactional
    public CharacterResponse createCharacter(CreateCharacterRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (owner.getRole() != Role.PLAYER) {
            throw new AccessDeniedException("Only players can create characters");
        }
        CharacterClass charClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        CharacterRace race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Character race not found"));

        PlayerCharacter character = PlayerCharacter.builder()
                .name(request.getName())
                .level(request.getLevel() != null ? request.getLevel() : 1)
                .characterClass(charClass)
                .race(race)
                .owner(owner)
                .build();
        character = characterRepository.save(character);

        List<StatType> allStatTypes = statTypeRepository.findAll();
        for (StatType st : allStatTypes) {
            CharacterStat stat = CharacterStat.builder()
                    .character(character)
                    .statType(st)
                    .value(10)
                    .build();
            characterStatRepository.save(stat);
            character.getStats().add(stat);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            InventorySlot invSlot = InventorySlot.builder()
                    .character(character)
                    .slot(slot)
                    .itemType(null)
                    .quantity(1)
                    .build();
            inventorySlotRepository.save(invSlot);
            character.getInventorySlots().add(invSlot);
        }

        return characterMapper.toResponse(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> listCharacters(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<PlayerCharacter> characters;
        switch (user.getRole()) {
            case PLAYER -> characters = characterRepository.findAllByOwnerId(user.getId());
            case GAME_MASTER -> characters = characterRepository.findAllByGameMasterId(user.getId());
            case ADMIN -> characters = characterRepository.findAll();
            default -> throw new AccessDeniedException("Unknown role");
        }
        return characters.stream().map(characterMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCharacterById(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);
        return characterMapper.toResponse(character);
    }

    @Transactional
    public CharacterResponse updateCharacter(UUID id, UpdateCharacterRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PLAYER || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the owning player can update this character");
        }
        if (request.getName() != null) character.setName(request.getName());
        if (request.getLevel() != null) character.setLevel(request.getLevel());
        if (request.getClassId() != null) {
            CharacterClass cc = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
            character.setCharacterClass(cc);
        }
        if (request.getRaceId() != null) {
            CharacterRace race = raceRepository.findById(request.getRaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Character race not found"));
            character.setRace(race);
        }
        character = characterRepository.save(character);
        return characterMapper.toResponse(character);
    }

    @Transactional
    public void deleteCharacter(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PLAYER || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the owning player can delete this character");
        }
        characterRepository.delete(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterStatResponse> getStats(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);
        return characterMapper.toStatResponseList(character.getStats());
    }

    @Transactional
    public CharacterStatResponse updateStatValue(UUID characterId, UUID statId, UpdateStatRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.PLAYER) {
            if (!character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You do not own this character");
            }
        } else if (user.getRole() == Role.GAME_MASTER) {
            if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
                throw new AccessDeniedException("This character's owner is not in any of your teams");
            }
        } else {
            throw new AccessDeniedException("Admins cannot edit stats");
        }

        CharacterStat stat = characterStatRepository.findById(statId)
                .orElseThrow(() -> new ResourceNotFoundException("Stat not found"));
        if (!stat.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Stat does not belong to this character");
        }
        stat.setValue(request.getValue());
        stat = characterStatRepository.save(stat);
        return characterMapper.toStatResponse(stat);
    }

    @Transactional(readOnly = true)
    public List<InventorySlotResponse> getInventory(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);
        return characterMapper.toInventorySlotResponseList(character.getInventorySlots());
    }

    @Transactional
    public InventorySlotResponse updateInventorySlot(UUID characterId, String slotName, UpdateInventorySlotRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PLAYER || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the owning player can update inventory");
        }
        EquipmentSlot equipSlot;
        try {
            equipSlot = EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid equipment slot: " + slotName);
        }
        InventorySlot invSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, equipSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory slot not found"));

        if (request.getItemTypeId() != null) {
            ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item type not found"));
            if (itemType.getSlot() != equipSlot) {
                throw new BadRequestException("Item type slot mismatch — expected " + equipSlot + " but item type requires " + itemType.getSlot());
            }
            invSlot.setItemType(itemType);
        } else {
            invSlot.setItemType(null);
        }
        invSlot.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        invSlot.setNotes(request.getNotes());
        invSlot = inventorySlotRepository.save(invSlot);
        return characterMapper.toInventorySlotResponse(invSlot);
    }

    private void enforceReadAccess(PlayerCharacter character, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        switch (user.getRole()) {
            case PLAYER -> {
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You do not own this character");
                }
            }
            case GAME_MASTER -> {
                if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
                    throw new AccessDeniedException("This character's owner is not in any of your teams");
                }
            }
            case ADMIN -> { /* admins can view all */ }
        }
    }
}
