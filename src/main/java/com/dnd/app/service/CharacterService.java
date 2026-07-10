package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Класс CharacterService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ContentCharacterClassRepository classRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final CharacterActiveEffectRepository activeEffectRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignContentService campaignContentService;
    private final CampaignService campaignService;
    private final CharacterMapper characterMapper;
    private final SpeciesService speciesService;
    private final ReferenceDataService referenceDataService;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final CharacterWalletRepository walletRepository;
    private final CharacterResourceRepository resourceRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final WebSocketEventService webSocketEventService;

    /**
     * Возвращает список для операции "list templates" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<CharacterResponse> listTemplates(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<PlayerCharacter> templates = characterRepository.findByOwnerIdAndCampaignIsNull(user.getId());
        return templates.stream().map(this::toResponse).toList();
    }

    /**
     * Возвращает результат операции "get template by id" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CharacterResponse getTemplateById(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (character.getCampaign() != null) {
            throw new BadRequestException("This character is bound to a campaign, not a template");
        }
        if (!character.getOwner().getId().equals(user.getId()) && user.getRole() != com.dnd.app.domain.enums.Role.ADMIN) {
            throw new AccessDeniedException("You can only view your own templates");
        }
        return toResponse(character);
    }

    /**
     * Выполняет операции "clone character to campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param templateId идентификатор template, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CharacterResponse cloneCharacterToCampaign(UUID campaignId, UUID templateId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        boolean isMember = campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaignId, user.getId());
        if (!isMember && user.getRole() != com.dnd.app.domain.enums.Role.ADMIN) {
            throw new AccessDeniedException("You are not a member of this campaign");
        }

        PlayerCharacter template = characterRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template character not found"));

        if (template.getCampaign() != null) {
            throw new BadRequestException("Source character is already bound to a campaign");
        }
        if (!template.getOwner().getId().equals(user.getId()) && user.getRole() != com.dnd.app.domain.enums.Role.ADMIN) {
            throw new AccessDeniedException("You can only clone your own templates");
        }

        PlayerCharacter copy = PlayerCharacter.builder()
                .name(template.getName())
                .totalLevel(template.getTotalLevel())
                .experience(template.getExperience())
                .race(template.getRace())
                .selectedLineageId(template.getSelectedLineageId())
                .raceSnapshotJson(template.getRaceSnapshotJson())
                .owner(user)
                .campaign(campaign)
                .alignment(template.getAlignment())
                .background(template.getBackground())
                .avatarUrl(template.getAvatarUrl())
                .armorClass(template.getArmorClass())
                .speed(template.getSpeed())
                .maxHp(template.getMaxHp())
                .currentHp(template.getMaxHp())
                .tempHp(0)
                .inspiration(false)
                .hitDiceType(template.getHitDiceType())
                .hitDiceTotal(template.getHitDiceTotal())
                .deathSaveSuccesses(0)
                .deathSaveFailures(0)
                .savingThrowProficiencyStatIdsJson(template.getSavingThrowProficiencyStatIdsJson())
                .biographyJson(template.getBiographyJson())
                .features(template.getFeatures())
                .attacksJson(template.getAttacksJson())
                .scoreMethod(template.getScoreMethod())
                .build();
        copy = characterRepository.saveAndFlush(copy);

        for (CharacterClassLevel ccl : template.getClassLevels()) {
            CharacterClassLevel newCcl = CharacterClassLevel.builder()
                    .characterId(copy.getId())
                    .classId(ccl.getClassId())
                    .classLevel(ccl.getClassLevel())
                    .build();
            classLevelRepository.saveAndFlush(newCcl);
            copy.getClassLevels().add(newCcl);
        }

        for (CharacterStat stat : template.getStats()) {
            CharacterStat newStat = CharacterStat.builder()
                    .character(copy)
                    .statType(stat.getStatType())
                    .value(stat.getValue())
                    .build();
            characterStatRepository.save(newStat);
            copy.getStats().add(newStat);
        }

        for (CharacterSkillProficiency sp : template.getSkillProficiencies()) {
            CharacterSkillProficiency newSp = CharacterSkillProficiency.builder()
                    .character(copy)
                    .skill(sp.getSkill())
                    .source(sp.getSource())
                    .build();
            skillProficiencyRepository.save(newSp);
            copy.getSkillProficiencies().add(newSp);
        }

        for (CharacterKnownSpell ks : template.getKnownSpells()) {
            CharacterKnownSpell newKs = CharacterKnownSpell.builder()
                    .character(copy)
                    .spell(ks.getSpell())
                    .build();
            knownSpellRepository.save(newKs);
            copy.getKnownSpells().add(newKs);
        }

        for (CharacterWallet wallet : walletRepository.findByCharacterId(template.getId())) {
            CharacterWallet newWallet = CharacterWallet.builder()
                    .character(copy)
                    .currencyType(wallet.getCurrencyType())
                    .amount(wallet.getAmount())
                    .build();
            walletRepository.save(newWallet);
        }

        for (CharacterResource resource : resourceRepository.findByCharacterId(template.getId())) {
            CharacterResource newResource = CharacterResource.builder()
                    .character(copy)
                    .resourceType(resource.getResourceType())
                    .currentValue(resource.getCurrentValue())
                    .build();
            resourceRepository.save(newResource);
        }

        log.info("Character cloned from template: templateId={}, copyId={}, campaign={}, by={}",
                template.getId(), copy.getId(), campaignId, username);

        return toResponse(copy);
    }

    /**
     * Удаляет результат операции "delete template" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteTemplate(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (character.getCampaign() != null) {
            throw new BadRequestException("This character is bound to a campaign, use campaign endpoint to delete");
        }
        if (!character.getOwner().getId().equals(user.getId()) && user.getRole() != com.dnd.app.domain.enums.Role.ADMIN) {
            throw new AccessDeniedException("You can only delete your own templates");
        }
        log.info("Template character deleted: id={}, name='{}', by user={}", characterId, character.getName(), username);
        characterRepository.delete(character);
    }

    /**
     * Создает результат операции "create character" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

        ContentCharacterClass charClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        com.dnd.app.domain.content.Species species =
                speciesService.getSelectableSpecies(campaign.getId(), request.getRaceId());

        PlayerCharacter character = PlayerCharacter.builder()
                .name(request.getName())
                .totalLevel(1)
                .experience(0L)
                .race(species)
                .selectedLineageId(null)
                .raceSnapshotJson(speciesService.buildSpeciesSnapshotJson(species))
                .owner(owner)
                .campaign(campaign)
                .build();
        character = characterRepository.saveAndFlush(character);
        log.info("Character created: id={}, name='{}', class='{}', speciesId={}, owner={}, campaignId={}",
                character.getId(), character.getName(), charClass.getNameRu(), species.getId(),
                username, campaign.getId());

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

    /**
     * Добавляет результат операции "add or update class level" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     */
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

    /**
     * Возвращает список для операции "list characters" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get character by id" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CharacterResponse getCharacterById(UUID id, String username) {
        PlayerCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);
        return toResponse(character);
    }

    /**
     * Проверяет требуемое условие операции "enforce character in campaign" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     */
    @Transactional(readOnly = true)
    public void enforceCharacterInCampaign(UUID characterId, UUID campaignId) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character not found in this campaign");
        }
    }

    /**
     * Обновляет результат операции "update character" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
        if (request.getPlayerName() != null) {
            character.setPlayerName(request.getPlayerName());
        }
        if (request.getProficiencies() != null) {
            character.setProficiencies(request.getProficiencies());
        }
        if (request.getEquipment() != null) {
            character.setEquipment(request.getEquipment());
        }
        if (request.getFeatures() != null) {
            character.setFeatures(request.getFeatures());
        }
        if (request.getAlignment() != null) {
            character.setAlignment(request.getAlignment());
        }
        if (request.getBiography() != null) {
            character.setBiographyJson(serializeCharacterPayload(request.getBiography()));
        }
        if (request.getAttacks() != null) {
            character.setAttacksJson(serializeCharacterPayload(request.getAttacks()));
        }
        if (request.getRaceId() != null) {
            if (character.getCampaign() == null) {
                throw new BadRequestException("Cannot change race for character without campaign");
            }
            com.dnd.app.domain.content.Species species =
                    speciesService.getSelectableSpecies(character.getCampaign().getId(), request.getRaceId());
            character.setRace(species);
            character.setSelectedLineageId(null);
            character.setRaceSnapshotJson(speciesService.buildSpeciesSnapshotJson(species));
        }

        character = characterRepository.save(character);
        CharacterResponse response = toResponse(character);
        UUID campaignId = character.getCampaign() != null ? character.getCampaign().getId() : null;
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CHARACTER_UPDATED, campaignId,
                character.getId(), response, user.getId());
        return response;
    }

    /**
     * Удаляет результат операции "delete character" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
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

    /**
     * Возвращает результат операции "get stats" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Обновляет результат операции "update stat value" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param statId идентификатор stat, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "modify hp" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CharacterResponse modifyHp(UUID campaignId, UUID characterId, ModifyHpRequest request, String username) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        enforceWriteAccess(character, user);

        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character not found in this campaign");
        }

        if (character.getMaxHp() == null) {
            throw new BadRequestException("Character max HP is not set");
        }

        if (request.getSetTempHp() != null) {
            character.setTempHp(request.getSetTempHp());
        }

        character.applyHpDelta(request.getAmount(), character.getMaxHp());
        character = characterRepository.save(character);

        int currentHp = character.getCurrentHp();
        int tempHp = character.getTempHp();
        log.info("HP modified: characterId={}, amount={}, setTempHp={}, newHp={}, newTempHp={}, by user={}",
                characterId, request.getAmount(), request.getSetTempHp(), currentHp, tempHp, username);

        webSocketEventService.sendCampaignEvent(WebSocketEventType.HP_CHANGED, character.getCampaign().getId(),
                characterId, Map.of("currentHp", currentHp, "tempHp", tempHp, "maxHp", character.getMaxHp()),
                user.getId());
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
        if (character.getRace() != null) {
            com.dnd.app.domain.content.Species sp = character.getRace();
            response.setRace(com.dnd.app.dto.response.CharacterRaceResponse.builder()
                    .id(sp.getId())
                    .name(sp.getNameEn() != null ? sp.getNameEn() : sp.getNameRu())
                    .description(sp.getDescription())
                    .build());
        }
        response.setRaceSnapshot(speciesService.parseSnapshot(character.getRaceSnapshotJson()));
        response.setCurrentHp(character.getCurrentHp());
        response.setMaxHp(character.getMaxHp());
        response.setTempHp(character.getTempHp());
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
                                    .skillName(sp.getSkill().getNameRu())
                                    .source(sp.getSource().name())
                                    .proficiencyLevel(sp.getProficiencyLevel() != null
                                            ? sp.getProficiencyLevel().name() : "PROFICIENT")
                                    .build())
                            .toList()
            );
        }

        if (character.getKnownSpells() != null) {
            response.setKnownSpells(
                    character.getKnownSpells().stream()
                            .map(ks -> com.dnd.app.dto.response.CharacterKnownSpellResponse.builder()
                                    .spellId(ks.getSpell().getId())
                                    .name(ks.getSpell().getNameRu())
                                    .level(ks.getSpell().getLevel())
                                    .school(ks.getSpell().getSchool() == null ? null : ks.getSpell().getSchool().getNameRu())
                                    .build())
                            .toList()
            );
        }

        if (character.getBiographyJson() != null) {
            try {
                response.setBiography(objectMapper.readValue(character.getBiographyJson(),
                        com.dnd.app.dto.response.BiographyResponse.class));
            } catch (Exception e) {
                log.warn(
                        "CharacterService#toResponse skipped biography payload: operation=character-response-map, characterId={}",
                        character.getId(),
                        e);
            }
        }

        if (character.getAttacksJson() != null) {
            try {
                response.setAttacks(objectMapper.readValue(character.getAttacksJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.dnd.app.dto.response.CharacterAttackResponse>>() {}));
            } catch (Exception e) {
                log.warn(
                        "CharacterService#toResponse skipped attacks payload: operation=character-response-map, characterId={}",
                        character.getId(),
                        e);
            }
        }

        return response;
    }

    private String serializeCharacterPayload(Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BadRequestException("Failed to serialize character payload", e);
        }
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
