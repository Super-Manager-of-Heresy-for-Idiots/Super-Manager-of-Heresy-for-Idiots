package com.dnd.app.service;

import com.dnd.app.domain.*;
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
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final CharacterActiveEffectRepository activeEffectRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignContentService campaignContentService;
    private final CampaignService campaignService;
    private final CharacterMapper characterMapper;
    private final RaceService raceService;
    private final ReferenceDataService referenceDataService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public CharacterResponse createCharacter(UUID campaignId, CreateCharacterRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (owner.getRole() != Role.PLAYER && owner.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only players can create characters");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (owner.getRole() == Role.PLAYER) {
            boolean isMember = campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaign.getId(), owner.getId());
            if (!isMember) {
                throw new AccessDeniedException("You are not a member of this campaign");
            }
        }

        if (!campaignContentService.isClassAvailableInCampaign(campaign.getId(), request.getClassId())) {
            throw new BadRequestException("Selected class is not available in this campaign");
        }

        CharacterClass charClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        CharacterRace race = raceService.getSelectableRace(campaign.getId(), request.getRaceId());
        raceService.validateLineageSelection(race, request.getSelectedLineageId());

        PlayerCharacter character = PlayerCharacter.builder()
                .name(request.getName())
                .totalLevel(1)
                .experience(0L)
                .race(race)
                .selectedLineageId(request.getSelectedLineageId())
                .raceSnapshotJson(raceService.buildRaceSnapshotJson(race, request.getSelectedLineageId()))
                .owner(owner)
                .campaign(campaign)
                .build();
        character = characterRepository.saveAndFlush(character);
        log.info("Character created: id={}, name='{}', class='{}', race='{}', lineageId={}, owner={}, campaignId={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(),
                request.getSelectedLineageId(), username, campaign.getId());

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

        return toResponse(character);
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        campaignService.enforceMembershipOrAdmin(campaign, user);

        List<PlayerCharacter> characters = characterRepository.findByCampaignId(campaignId);
        return characters.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponse getCharacterById(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);
        return toResponse(character);
    }

    @Transactional
    public CharacterResponse updateCharacter(UUID id, UpdateCharacterRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        enforceWriteAccess(character, user);

        if (request.getName() != null) {
            character.setName(request.getName());
        }
        if (request.getRaceId() != null) {
            if (character.getCampaign() == null) {
                throw new BadRequestException("Cannot change race for character without campaign");
            }
            CharacterRace race = raceService.getSelectableRace(character.getCampaign().getId(), request.getRaceId());
            raceService.validateLineageSelection(race, request.getSelectedLineageId());
            character.setRace(race);
            character.setSelectedLineageId(request.getSelectedLineageId());
            character.setRaceSnapshotJson(raceService.buildRaceSnapshotJson(race, request.getSelectedLineageId()));
        } else if (request.getSelectedLineageId() != null) {
            raceService.validateLineageSelection(character.getRace(), request.getSelectedLineageId());
            character.setSelectedLineageId(request.getSelectedLineageId());
            character.setRaceSnapshotJson(raceService.buildRaceSnapshotJson(character.getRace(), request.getSelectedLineageId()));
        }

        character = characterRepository.save(character);
        return toResponse(character);
    }

    @Transactional
    public void deleteCharacter(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        enforceWriteAccess(character, user);
        log.info("Character deleted: id={}, name='{}', by user={}", id, character.getName(), username);
        characterRepository.delete(character);
    }

    @Transactional(readOnly = true)
    public List<CharacterStatResponse> getStats(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);

        List<CharacterActiveEffect> activeEffects = activeEffectRepository.findByCharacterId(characterId);

        return character.getStats().stream().map(stat -> {
            CharacterStatResponse resp = characterMapper.toStatResponse(stat);
            List<StatModifierDetail> modifiers = new java.util.ArrayList<>();
            int totalMod = 0;
            for (CharacterActiveEffect effect : activeEffects) {
                BuffDebuff bd = effect.getBuffDebuff();
                if (bd.getTargetStat() != null && bd.getModifierValue() != null
                        && bd.getTargetStat().getId().equals(stat.getStatType().getId())) {
                    modifiers.add(StatModifierDetail.builder()
                            .source(bd.getName())
                            .modifierValue(bd.getModifierValue())
                            .build());
                    totalMod += bd.getModifierValue();
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
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        enforceWriteAccess(character, user);

        CharacterStat stat = characterStatRepository.findById(statId)
                .orElseThrow(() -> new ResourceNotFoundException("Character stat not found"));
        if (!stat.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Character stat does not belong to this character");
        }
        stat.setValue(request.getValue());
        stat = characterStatRepository.save(stat);
        return characterMapper.toStatResponse(stat);
    }

    @Transactional
    public CharacterResponse modifyHp(UUID campaignId, UUID characterId, ModifyHpRequest request, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        enforceWriteAccess(character, user);

        if (character.getMaxHp() == null) {
            throw new BadRequestException("Character max HP is not set");
        }

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int newHp = Math.max(0, Math.min(currentHp + request.getAmount(), character.getMaxHp()));
        character.setCurrentHp(newHp);
        character = characterRepository.save(character);

        log.info("HP modified: characterId={}, amount={}, newHp={}, by user={}", characterId, request.getAmount(), newHp, username);
        return toResponse(character);
    }

    private void enforceReadAccess(PlayerCharacter character, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        switch (user.getRole()) {
            case PLAYER -> {
                if (character.getCampaign() != null
                        && campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                    return;
                }
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You cannot read this character");
                }
            }
            case GAME_MASTER -> {
                if (character.getCampaign() == null
                        || !campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
                    throw new AccessDeniedException("This character is not in your campaign");
                }
            }
            case ADMIN -> { /* admins can view all */ }
        }
    }

    private CharacterResponse toResponse(PlayerCharacter character) {
        CharacterResponse response = characterMapper.toResponse(character);
        response.setRaceSnapshot(raceService.parseSnapshot(character.getRaceSnapshotJson()));
        response.setCurrentHp(character.getCurrentHp());
        response.setMaxHp(character.getMaxHp());
        response.setAlignment(character.getAlignment());
        response.setAvatarUrl(character.getAvatarUrl());
        response.setArmorClass(character.getArmorClass());
        response.setSpeed(character.getSpeed());
        response.setInspiration(character.getInspiration());
        response.setHitDiceType(character.getHitDiceType());
        response.setHitDiceTotal(character.getHitDiceTotal());
        response.setDeathSaveSuccesses(character.getDeathSaveSuccesses());
        response.setDeathSaveFailures(character.getDeathSaveFailures());
        response.setFeatures(character.getFeatures());

        if (character.getBackground() != null) {
            response.setBackground(referenceDataService.mapBackground(character.getBackground()));
        }

        List<String> saveNames = referenceDataService.parseJsonStringList(character.getSavingThrowProficiencyStatIdsJson());
        response.setSavingThrowProficiencyStatNames(saveNames);

        if (character.getSkillProficiencies() != null) {
            response.setSkillProficiencies(
                    character.getSkillProficiencies().stream()
                            .map(sp -> com.dnd.app.dto.response.CharacterSkillProficiencyResponse.builder()
                                    .skillId(sp.getSkill().getId())
                                    .skillName(sp.getSkill().getName())
                                    .source(sp.getSource().name())
                                    .build())
                            .toList()
            );
        }

        if (character.getKnownSpells() != null) {
            response.setKnownSpells(
                    character.getKnownSpells().stream()
                            .map(ks -> com.dnd.app.dto.response.CharacterKnownSpellResponse.builder()
                                    .spellId(ks.getSpell().getId())
                                    .name(ks.getSpell().getName())
                                    .level(ks.getSpell().getLevel())
                                    .school(ks.getSpell().getSchool())
                                    .build())
                            .toList()
            );
        }

        if (character.getBiographyJson() != null) {
            try {
                response.setBiography(objectMapper.readValue(character.getBiographyJson(),
                        com.dnd.app.dto.response.BiographyResponse.class));
            } catch (Exception ignored) {}
        }

        if (character.getAttacksJson() != null) {
            try {
                response.setAttacks(objectMapper.readValue(character.getAttacksJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.dnd.app.dto.response.CharacterAttackResponse>>() {}));
            } catch (Exception ignored) {}
        }

        return response;
    }

    private void enforceWriteAccess(PlayerCharacter character, User user) {
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        boolean isCampaignGM = user.getRole() == Role.GAME_MASTER && character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!isOwner && !isCampaignGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("No permission to modify this character");
        }
    }
}
