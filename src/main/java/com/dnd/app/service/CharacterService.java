package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.request.ModifyHpRequest;
import com.dnd.app.dto.request.UpdateCharacterRequest;
import com.dnd.app.dto.request.UpdateStatRequest;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
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
    private final CharacterConditionRepository charCondRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignContentService campaignContentService;
    private final CampaignService campaignService;
    private final CharacterMapper characterMapper;

    @Transactional
    public CharacterResponse createCharacter(UUID campaignId, CreateCharacterRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (owner.getRole() != Role.PLAYER && owner.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только игроки могут создавать персонажей");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Кампания не найдена"));

        if (owner.getRole() == Role.PLAYER) {
            boolean isMember = campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaign.getId(), owner.getId());
            if (!isMember) {
                throw new AccessDeniedException("Вы не являетесь участником этой кампании");
            }
        }

        if (!campaignContentService.isClassAvailableInCampaign(campaign.getId(), request.getClassId())) {
            throw new BadRequestException("Выбранный класс недоступен в контексте этой кампании");
        }
        if (!campaignContentService.isRaceAvailableInCampaign(campaign.getId(), request.getRaceId())) {
            throw new BadRequestException("Выбранная раса недоступна в контексте этой кампании");
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
                .campaign(campaign)
                .build();
        character = characterRepository.saveAndFlush(character);
        log.info("Character created: id={}, name='{}', class='{}', race='{}', owner={}, campaignId={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(), username, campaign.getId());

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
    public List<CharacterResponse> listCharacters(UUID campaignId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Кампания не найдена"));
        campaignService.enforceMembershipOrAdmin(campaign, user);

        List<PlayerCharacter> characters;
        switch (user.getRole()) {
            case PLAYER -> characters = characterRepository.findByCampaignIdAndOwnerId(campaignId, user.getId());
            case GAME_MASTER, ADMIN -> characters = characterRepository.findByCampaignId(campaignId);
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
        enforceWriteAccess(character, user);

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
        enforceWriteAccess(character, user);
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
        enforceWriteAccess(character, user);

        CharacterStat stat = characterStatRepository.findById(statId)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика персонажа не найдена"));
        if (!stat.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Характеристика не относится к этому персонажу");
        }
        stat.setValue(request.getValue());
        stat = characterStatRepository.save(stat);
        return characterMapper.toStatResponse(stat);
    }

    @Transactional
    public CharacterResponse modifyHp(UUID campaignId, UUID characterId, ModifyHpRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        enforceWriteAccess(character, user);

        if (character.getMaxHp() == null) {
            throw new BadRequestException("Максимальные HP персонажа не установлены");
        }

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int newHp = Math.max(0, Math.min(currentHp + request.getAmount(), character.getMaxHp()));
        character.setCurrentHp(newHp);
        character = characterRepository.save(character);

        log.info("HP modified: characterId={}, amount={}, newHp={}, by user={}", characterId, request.getAmount(), newHp, username);
        return characterMapper.toResponse(character);
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
                if (character.getCampaign() == null
                        || !campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
                    throw new AccessDeniedException("Этот персонаж не принадлежит вашей кампании");
                }
            }
            case ADMIN -> { /* admins can view all */ }
        }
    }

    private void enforceWriteAccess(PlayerCharacter character, User user) {
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        boolean isCampaignGM = user.getRole() == Role.GAME_MASTER && character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!isOwner && !isCampaignGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на изменение этого персонажа");
        }
    }
}
