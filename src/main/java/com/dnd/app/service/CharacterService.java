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
import com.dnd.app.dto.response.StatModifierDetail;
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
public class CharacterService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final InventorySlotRepository inventorySlotRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final CharacterConditionRepository charCondRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CharacterMapper characterMapper;

    @Transactional
    public CharacterResponse createCharacter(CreateCharacterRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (owner.getRole() != Role.PLAYER && owner.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только игроки могут создавать персонажей");
        }
        CharacterClass charClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        CharacterRace race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Раса персонажа не найдена"));

        PlayerCharacter character = PlayerCharacter.builder()
                .name(request.getName())
                .totalLevel(1)
                .experience(0L)
                .race(race)
                .owner(owner)
                .build();
        character = characterRepository.save(character);
        log.info("Character created: id={}, name='{}', class='{}', race='{}', owner={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(), username);

        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(character.getId())
                .classId(charClass.getId())
                .classLevel(1)
                .build();
        classLevelRepository.save(ccl);
        character.getClassLevels().add(ccl);

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
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        List<PlayerCharacter> characters;
        switch (user.getRole()) {
            case PLAYER -> characters = characterRepository.findAllByOwnerId(user.getId());
            case GAME_MASTER -> characters = characterRepository.findAllByGameMasterId(user.getId());
            case ADMIN -> characters = characterRepository.findAll();
            default -> throw new AccessDeniedException("Неизвестная роль");
        }
        return characters.stream().map(characterMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCharacterById(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);
        return characterMapper.toResponse(character);
    }

    @Transactional
    public CharacterResponse updateCharacter(UUID id, UpdateCharacterRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец может обновлять этого персонажа");
        }
        if (request.getName() != null) character.setName(request.getName());
        if (request.getRaceId() != null) {
            CharacterRace race = raceRepository.findById(request.getRaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Раса персонажа не найдена"));
            character.setRace(race);
        }
        character = characterRepository.save(character);
        return characterMapper.toResponse(character);
    }

    @Transactional
    public void deleteCharacter(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец может удалить этого персонажа");
        }
        log.info("Character deleted: id={}, name='{}', by user={}", id, character.getName(), username);
        characterRepository.delete(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterStatResponse> getStats(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);

        List<CharacterCondition> activeConditions =
                charCondRepository.findAllByCharacterIdAndActiveTrue(characterId);

        return character.getStats().stream().map(stat -> {
            CharacterStatResponse resp = characterMapper.toStatResponse(stat);
            List<StatModifierDetail> modifiers = new java.util.ArrayList<>();
            int totalMod = 0;
            for (CharacterCondition cc : activeConditions) {
                for (ConditionModifier cm : cc.getCondition().getModifiers()) {
                    if (cm.getStatType().getId().equals(stat.getStatType().getId())) {
                        modifiers.add(StatModifierDetail.builder()
                                .source(cc.getCondition().getName())
                                .modifierValue(cm.getModifierValue())
                                .build());
                        totalMod += cm.getModifierValue();
                    }
                }
            }
            resp.setEffectiveValue(stat.getValue() + totalMod);
            resp.setActiveModifiers(modifiers.isEmpty() ? null : modifiers);
            return resp;
        }).toList();
    }

    @Transactional
    public CharacterStatResponse updateStatValue(UUID characterId, UUID statId, UpdateStatRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getRole() == Role.PLAYER) {
            if (!character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("Этот персонаж вам не принадлежит");
            }
        } else if (user.getRole() == Role.GAME_MASTER) {
            if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
                throw new AccessDeniedException("Владелец этого персонажа не состоит ни в одной из ваших команд");
            }
        }
        // ADMIN can edit any stat

        CharacterStat stat = characterStatRepository.findById(statId)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика персонажа не найдена"));
        if (!stat.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Характеристика не относится к этому персонажу");
        }
        stat.setValue(request.getValue());
        stat = characterStatRepository.save(stat);
        return characterMapper.toStatResponse(stat);
    }

    @Transactional(readOnly = true)
    public List<InventorySlotResponse> getInventory(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);
        return characterMapper.toInventorySlotResponseList(character.getInventorySlots());
    }

    @Transactional
    public InventorySlotResponse updateInventorySlot(UUID characterId, String slotName, UpdateInventorySlotRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец может обновлять инвентарь");
        }
        EquipmentSlot equipSlot;
        try {
            equipSlot = EquipmentSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный слот экипировки: " + slotName);
        }
        InventorySlot invSlot = inventorySlotRepository.findByCharacterIdAndSlot(characterId, equipSlot)
                .orElseThrow(() -> new ResourceNotFoundException("Слот инвентаря не найден"));

        if (request.getItemTypeId() != null) {
            ItemType itemType = itemTypeRepository.findById(request.getItemTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
            if (itemType.getSlot() != equipSlot) {
                throw new BadRequestException("Несоответствие слота типа предмета — ожидался " +
                        com.dnd.app.util.ResponseLocalizer.equipmentSlot(equipSlot) +
                        ", а типу предмета нужен " + com.dnd.app.util.ResponseLocalizer.equipmentSlot(itemType.getSlot()));
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
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        switch (user.getRole()) {
            case PLAYER -> {
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Этот персонаж вам не принадлежит");
                }
            }
            case GAME_MASTER -> {
                if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
                    throw new AccessDeniedException("Владелец этого персонажа не состоит ни в одной из ваших команд");
                }
            }
            case ADMIN -> { /* admins can view all */ }
        }
    }
}
