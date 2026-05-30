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
import java.util.Optional;
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
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamContentService teamContentService;
    private final CharacterMapper characterMapper;

    @Transactional
    public CharacterResponse createCharacter(CreateCharacterRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (owner.getRole() != Role.PLAYER && owner.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только игроки могут создавать персонажей");
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));

        if (owner.getRole() == Role.PLAYER) {
            boolean isMember = teamMemberRepository.existsByIdTeamIdAndIdPlayerId(team.getId(), owner.getId());
            if (!isMember) {
                throw new AccessDeniedException("Вы не являетесь участником этой команды");
            }
        }

        if (!teamContentService.isClassAvailableInTeam(team.getId(), request.getClassId())) {
            throw new BadRequestException("Выбранный класс недоступен в контексте этой команды");
        }
        if (!teamContentService.isRaceAvailableInTeam(team.getId(), request.getRaceId())) {
            throw new BadRequestException("Выбранная раса недоступна в контексте этой команды");
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
                .team(team)
                .build();
        character = characterRepository.saveAndFlush(character);
        log.info("Character created: id={}, name='{}', class='{}', race='{}', owner={}, teamId={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(), username, team.getId());

        addOrUpdateClassLevel(character, charClass.getId(), 1);

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

    public void addOrUpdateClassLevel(PlayerCharacter character, UUID classId, int level) {
        CharacterClassLevelId cclId = new CharacterClassLevelId(character.getId(), classId);
        Optional<CharacterClassLevel> existing = classLevelRepository.findById(cclId);

        if (existing.isPresent()) {
            CharacterClassLevel ccl = existing.get();
            ccl.setClassLevel(level);
            classLevelRepository.save(ccl);
        } else {
            CharacterClassLevel ccl = CharacterClassLevel.builder()
                    .characterId(character.getId())
                    .classId(classId)
                    .classLevel(level)
                    .build();
            ccl = classLevelRepository.saveAndFlush(ccl);
            character.getClassLevels().add(ccl);
        }
    }

    @Transactional(readOnly = true)
    public List<CharacterResponse> listCharacters(String username, UUID teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        List<PlayerCharacter> characters;
        switch (user.getRole()) {
            case PLAYER -> {
                if (teamId != null) {
                    characters = characterRepository.findAllByOwnerIdAndTeamId(user.getId(), teamId);
                } else {
                    characters = characterRepository.findAllByOwnerId(user.getId());
                }
            }
            case GAME_MASTER -> {
                if (teamId != null) {
                    characters = characterRepository.findAllByGameMasterIdAndTeamId(user.getId(), teamId);
                } else {
                    characters = characterRepository.findAllByGameMasterId(user.getId());
                }
            }
            case ADMIN -> {
                if (teamId != null) {
                    characters = characterRepository.findAllByTeamId(teamId);
                } else {
                    characters = characterRepository.findAll();
                }
            }
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
        boolean isTeamGM = user.getRole() == Role.GAME_MASTER && character.getTeam() != null
                && character.getTeam().getGameMaster().getId().equals(user.getId());
        if (!isOwner && !isTeamGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на обновление этого персонажа");
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
        boolean isTeamGM = user.getRole() == Role.GAME_MASTER && character.getTeam() != null
                && character.getTeam().getGameMaster().getId().equals(user.getId());
        if (!isOwner && !isTeamGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на удаление этого персонажа");
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
            if (character.getTeam() == null || !character.getTeam().getGameMaster().getId().equals(user.getId())) {
                throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
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
                throw new BadRequestException("Несоответствие слота типа предмета — ожидался " + equipSlot + ", а типу предмета нужен " + itemType.getSlot());
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
                if (character.getTeam() == null || !character.getTeam().getGameMaster().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
                }
            }
            case ADMIN -> { /* admins can view all */ }
        }
    }
}
