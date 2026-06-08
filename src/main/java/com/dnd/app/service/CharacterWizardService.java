package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ScoreMethod;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.dto.request.CreateFullCharacterRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CharacterMapper;
import com.dnd.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterWizardService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final BackgroundRepository backgroundRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final SpellRepository spellRepository;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final CharacterWalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignService campaignService;
    private final CampaignContentService campaignContentService;
    private final RaceService raceService;
    private final ReferenceDataService referenceDataService;
    private final CharacterMapper characterMapper;
    private final ObjectMapper objectMapper;

    private static final Set<Integer> STANDARD_ARRAY = Set.of(15, 14, 13, 12, 10, 8);
    private static final Map<Integer, Integer> POINT_BUY_COST = Map.of(
            8, 0, 9, 1, 10, 2, 11, 3, 12, 4, 13, 5, 14, 7, 15, 9
    );

    @Transactional
    public CharacterResponse createFullCharacter(UUID campaignId, CreateFullCharacterRequest req, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        boolean isMember = campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaign.getId(), owner.getId());
        if (!isMember) {
            throw new BadRequestException("You are not a member of this campaign");
        }

        if (!campaignContentService.isClassAvailableInCampaign(campaignId, req.getClassId())) {
            throw new BadRequestException("Selected class is not available in this campaign");
        }

        CharacterClass charClass = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        CharacterRace race = raceService.getSelectableRace(campaignId, req.getRaceId());

        if (req.getSubraceId() != null) {
            raceService.validateLineageSelection(race, req.getSubraceId());
        } else if (Boolean.TRUE.equals(race.getLineageRequired())) {
            throw new BadRequestException("This race requires a subrace/lineage selection");
        }

        Background background = backgroundRepository.findById(req.getBackgroundId())
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));

        if (req.getLevel() < 1 || req.getLevel() > 20) {
            throw new BadRequestException("Level must be between 1 and 20");
        }

        ScoreMethod scoreMethod = parseScoreMethod(req.getScoreMethod());
        validateAbilityScores(req.getAbilityScores(), scoreMethod);
        validateSkillChoices(req.getChosenSkillProficiencyIds(), charClass, background);
        validateSpells(req.getCantripIds(), req.getSpellIds(), charClass, req.getLevel());

        Map<UUID, Integer> baseScores = req.getAbilityScores().stream()
                .collect(Collectors.toMap(
                        CreateFullCharacterRequest.AbilityScoreEntry::getStatId,
                        CreateFullCharacterRequest.AbilityScoreEntry::getBaseValue));

        Map<String, Integer> racialBonuses = getRacialBonuses(race, req.getSubraceId());

        List<StatType> allStatTypes = statTypeRepository.findAll();
        Map<String, StatType> statByName = allStatTypes.stream()
                .collect(Collectors.toMap(StatType::getName, s -> s));

        int walkSpeed = getWalkSpeed(race);
        int hitDie = charClass.getHitDie() != null ? charClass.getHitDie() : 8;
        int conModifier = getAbilityModifier(getFinalStatValue(baseScores, racialBonuses, statByName, "Constitution"));
        int maxHp = calculateMaxHp(hitDie, req.getLevel(), conModifier);
        int dexModifier = getAbilityModifier(getFinalStatValue(baseScores, racialBonuses, statByName, "Dexterity"));
        int armorClass = 10 + dexModifier;

        List<String> savingThrows = referenceDataService.parseJsonStringList(charClass.getSavingThrowStatIdsJson());
        String biographyJson = serializeBiography(req.getBiography());

        PlayerCharacter character = PlayerCharacter.builder()
                .name(req.getName())
                .totalLevel(req.getLevel())
                .experience(0L)
                .race(race)
                .selectedLineageId(req.getSubraceId())
                .raceSnapshotJson(raceService.buildRaceSnapshotJson(race, req.getSubraceId()))
                .owner(owner)
                .campaign(campaign)
                .playerName(req.getPlayerName())
                .proficiencies(req.getProficiencies())
                .equipment(req.getEquipment())
                .alignment(req.getAlignment())
                .background(background)
                .avatarUrl(req.getAvatar())
                .armorClass(armorClass)
                .speed(walkSpeed)
                .maxHp(maxHp)
                .currentHp(maxHp)
                .hitDiceType("d" + hitDie)
                .hitDiceTotal(req.getLevel() + "d" + hitDie)
                .deathSaveSuccesses(0)
                .deathSaveFailures(0)
                .savingThrowProficiencyStatIdsJson(serializeStringList(savingThrows))
                .biographyJson(biographyJson)
                .scoreMethod(scoreMethod)
                .build();

        character = characterRepository.saveAndFlush(character);

        CharacterClassLevelId cclId = new CharacterClassLevelId(character.getId(), charClass.getId());
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(character.getId())
                .classId(charClass.getId())
                .classLevel(req.getLevel())
                .build();
        classLevelRepository.saveAndFlush(ccl);
        character.getClassLevels().add(ccl);

        for (StatType st : allStatTypes) {
            int base = baseScores.getOrDefault(st.getId(), 10);
            int racial = racialBonuses.getOrDefault(st.getName(), 0);
            CharacterStat stat = CharacterStat.builder()
                    .character(character)
                    .statType(st)
                    .value(base + racial)
                    .build();
            characterStatRepository.save(stat);
            character.getStats().add(stat);
        }

        saveSkillProficiencies(character, req.getChosenSkillProficiencyIds(), background);

        if (req.getCantripIds() != null) {
            saveKnownSpells(character, req.getCantripIds());
        }
        if (req.getSpellIds() != null) {
            saveKnownSpells(character, req.getSpellIds());
        }

        if (req.getStartingCoins() != null) {
            for (var coin : req.getStartingCoins()) {
                CurrencyType ct = currencyTypeRepository.findById(coin.getCurrencyTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Currency type not found"));
                CharacterWallet wallet = CharacterWallet.builder()
                        .character(character)
                        .currencyType(ct)
                        .amount(BigDecimal.valueOf(coin.getAmount()))
                        .build();
                walletRepository.save(wallet);
            }
        }

        log.info("Full character created: id={}, name='{}', class='{}', race='{}', level={}, owner={}, campaign={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(),
                req.getLevel(), username, campaignId);

        return buildFullResponse(character);
    }

    @Transactional
    public CharacterResponse createVanillaCharacter(CreateFullCharacterRequest req, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CharacterClass charClass = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        if (charClass.getHomebrew() != null) {
            throw new BadRequestException("Homebrew classes cannot be used in vanilla characters");
        }

        CharacterRace race = raceService.getSelectableVanillaRace(req.getRaceId());

        if (req.getSubraceId() != null) {
            raceService.validateLineageSelection(race, req.getSubraceId());
        } else if (Boolean.TRUE.equals(race.getLineageRequired())) {
            throw new BadRequestException("This race requires a subrace/lineage selection");
        }

        Background background = backgroundRepository.findById(req.getBackgroundId())
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        if (background.getHomebrew() != null) {
            throw new BadRequestException("Homebrew backgrounds cannot be used in vanilla characters");
        }

        if (req.getLevel() < 1 || req.getLevel() > 20) {
            throw new BadRequestException("Level must be between 1 and 20");
        }

        ScoreMethod scoreMethod = parseScoreMethod(req.getScoreMethod());
        validateAbilityScores(req.getAbilityScores(), scoreMethod);
        validateSkillChoices(req.getChosenSkillProficiencyIds(), charClass, background);
        validateSpells(req.getCantripIds(), req.getSpellIds(), charClass, req.getLevel());

        if (req.getSpellIds() != null) {
            for (UUID spellId : req.getSpellIds()) {
                Spell spell = spellRepository.findById(spellId)
                        .orElseThrow(() -> new BadRequestException("Spell not found: " + spellId));
                if (spell.getHomebrew() != null) {
                    throw new BadRequestException("Homebrew spells cannot be used in vanilla characters");
                }
            }
        }

        Map<UUID, Integer> baseScores = req.getAbilityScores().stream()
                .collect(Collectors.toMap(
                        CreateFullCharacterRequest.AbilityScoreEntry::getStatId,
                        CreateFullCharacterRequest.AbilityScoreEntry::getBaseValue));

        Map<String, Integer> racialBonuses = getRacialBonuses(race, req.getSubraceId());

        List<StatType> allStatTypes = statTypeRepository.findAll();
        Map<String, StatType> statByName = allStatTypes.stream()
                .collect(Collectors.toMap(StatType::getName, s -> s));

        int walkSpeed = getWalkSpeed(race);
        int hitDie = charClass.getHitDie() != null ? charClass.getHitDie() : 8;
        int conModifier = getAbilityModifier(getFinalStatValue(baseScores, racialBonuses, statByName, "Constitution"));
        int maxHp = calculateMaxHp(hitDie, req.getLevel(), conModifier);
        int dexModifier = getAbilityModifier(getFinalStatValue(baseScores, racialBonuses, statByName, "Dexterity"));
        int armorClass = 10 + dexModifier;

        List<String> savingThrows = referenceDataService.parseJsonStringList(charClass.getSavingThrowStatIdsJson());
        String biographyJson = serializeBiography(req.getBiography());

        PlayerCharacter character = PlayerCharacter.builder()
                .name(req.getName())
                .totalLevel(req.getLevel())
                .experience(0L)
                .race(race)
                .selectedLineageId(req.getSubraceId())
                .raceSnapshotJson(raceService.buildRaceSnapshotJson(race, req.getSubraceId()))
                .owner(owner)
                .playerName(req.getPlayerName())
                .proficiencies(req.getProficiencies())
                .equipment(req.getEquipment())
                .alignment(req.getAlignment())
                .background(background)
                .avatarUrl(req.getAvatar())
                .armorClass(armorClass)
                .speed(walkSpeed)
                .maxHp(maxHp)
                .currentHp(maxHp)
                .hitDiceType("d" + hitDie)
                .hitDiceTotal(req.getLevel() + "d" + hitDie)
                .deathSaveSuccesses(0)
                .deathSaveFailures(0)
                .savingThrowProficiencyStatIdsJson(serializeStringList(savingThrows))
                .biographyJson(biographyJson)
                .scoreMethod(scoreMethod)
                .build();

        character = characterRepository.saveAndFlush(character);

        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(character.getId())
                .classId(charClass.getId())
                .classLevel(req.getLevel())
                .build();
        classLevelRepository.saveAndFlush(ccl);
        character.getClassLevels().add(ccl);

        for (StatType st : allStatTypes) {
            int base = baseScores.getOrDefault(st.getId(), 10);
            int racial = racialBonuses.getOrDefault(st.getName(), 0);
            CharacterStat stat = CharacterStat.builder()
                    .character(character)
                    .statType(st)
                    .value(base + racial)
                    .build();
            characterStatRepository.save(stat);
            character.getStats().add(stat);
        }

        saveSkillProficiencies(character, req.getChosenSkillProficiencyIds(), background);

        if (req.getCantripIds() != null) {
            saveKnownSpells(character, req.getCantripIds());
        }
        if (req.getSpellIds() != null) {
            saveKnownSpells(character, req.getSpellIds());
        }

        if (req.getStartingCoins() != null) {
            for (var coin : req.getStartingCoins()) {
                CurrencyType ct = currencyTypeRepository.findById(coin.getCurrencyTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Currency type not found"));
                CharacterWallet wallet = CharacterWallet.builder()
                        .character(character)
                        .currencyType(ct)
                        .amount(java.math.BigDecimal.valueOf(coin.getAmount()))
                        .build();
                walletRepository.save(wallet);
            }
        }

        log.info("Vanilla character created: id={}, name='{}', class='{}', race='{}', level={}, owner={}",
                character.getId(), character.getName(), charClass.getName(), race.getName(),
                req.getLevel(), username);

        return buildFullResponse(character);
    }

    private CharacterResponse buildFullResponse(PlayerCharacter character) {
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

        response.setSkillProficiencies(
                character.getSkillProficiencies().stream()
                        .map(sp -> CharacterSkillProficiencyResponse.builder()
                                .skillId(sp.getSkill().getId())
                                .skillName(sp.getSkill().getName())
                                .source(sp.getSource().name())
                                .build())
                        .toList()
        );

        response.setKnownSpells(
                character.getKnownSpells().stream()
                        .map(ks -> CharacterKnownSpellResponse.builder()
                                .spellId(ks.getSpell().getId())
                                .name(ks.getSpell().getName())
                                .level(ks.getSpell().getLevel())
                                .school(ks.getSpell().getSchool())
                                .build())
                        .toList()
        );

        response.setBiography(parseBiography(character.getBiographyJson()));
        response.setAttacks(parseAttacks(character.getAttacksJson()));

        return response;
    }

    // --- Validation ---

    private ScoreMethod parseScoreMethod(String method) {
        try {
            return ScoreMethod.valueOf(method);
        } catch (Exception e) {
            throw new BadRequestException("Invalid score method: " + method + ". Must be STANDARD_ARRAY, POINT_BUY, or ROLL");
        }
    }

    private void validateAbilityScores(List<CreateFullCharacterRequest.AbilityScoreEntry> scores, ScoreMethod method) {
        if (scores == null || scores.size() != 6) {
            throw new BadRequestException("Exactly 6 ability scores required");
        }

        Set<UUID> seenStats = new HashSet<>();
        for (var entry : scores) {
            if (!seenStats.add(entry.getStatId())) {
                throw new BadRequestException("Duplicate stat ID in ability scores");
            }
        }

        List<Integer> values = scores.stream().map(CreateFullCharacterRequest.AbilityScoreEntry::getBaseValue).toList();

        switch (method) {
            case STANDARD_ARRAY -> {
                List<Integer> sorted = new ArrayList<>(values);
                Collections.sort(sorted);
                List<Integer> expected = new ArrayList<>(STANDARD_ARRAY);
                Collections.sort(expected);
                if (!sorted.equals(expected)) {
                    throw new BadRequestException("Standard Array values must be exactly {15, 14, 13, 12, 10, 8}");
                }
            }
            case POINT_BUY -> {
                int totalCost = 0;
                for (int v : values) {
                    if (v < 8 || v > 15) {
                        throw new BadRequestException("Point Buy: each score must be between 8 and 15");
                    }
                    totalCost += POINT_BUY_COST.get(v);
                }
                if (totalCost > 27) {
                    throw new BadRequestException("Point Buy: total cost (" + totalCost + ") exceeds 27");
                }
            }
            case ROLL -> {
                for (int v : values) {
                    if (v < 3 || v > 18) {
                        throw new BadRequestException("Roll: each score must be between 3 and 18");
                    }
                }
            }
        }
    }

    private void validateSkillChoices(List<UUID> chosenIds, CharacterClass charClass, Background background) {
        if (chosenIds == null) chosenIds = List.of();

        int expected = charClass.getSkillChoiceCount() != null ? charClass.getSkillChoiceCount() : 2;
        if (chosenIds.size() != expected) {
            throw new BadRequestException("Expected " + expected + " skill choices, got " + chosenIds.size());
        }

        List<String> allowedNames = referenceDataService.parseJsonStringList(charClass.getSkillChoiceOptionIdsJson());
        Set<String> allowedSet = new HashSet<>(allowedNames);

        List<String> bgSkillNames = referenceDataService.parseJsonStringList(background.getSkillProficiencyIdsJson());
        Set<String> bgSkillSet = new HashSet<>(bgSkillNames);

        for (UUID skillId : chosenIds) {
            ProficiencySkill ps = proficiencySkillRepository.findById(skillId)
                    .orElseThrow(() -> new BadRequestException("Proficiency skill not found: " + skillId));
            if (!allowedSet.contains(ps.getName())) {
                throw new BadRequestException("Skill '" + ps.getName() + "' is not available for this class");
            }
            if (bgSkillSet.contains(ps.getName())) {
                throw new BadRequestException("Skill '" + ps.getName() + "' is already granted by background");
            }
        }
    }

    private void validateSpells(List<UUID> cantripIds, List<UUID> spellIds, CharacterClass charClass, int level) {
        if (!Boolean.TRUE.equals(charClass.getIsSpellcaster())) {
            if ((cantripIds != null && !cantripIds.isEmpty()) || (spellIds != null && !spellIds.isEmpty())) {
                throw new BadRequestException("Non-spellcaster class cannot have spells");
            }
            return;
        }

        if (cantripIds != null && !cantripIds.isEmpty()) {
            if (!Boolean.TRUE.equals(charClass.getHasCantrips())) {
                throw new BadRequestException("This class does not have cantrips");
            }
            int maxCantrips = getMaxCantrips(charClass.getName(), level);
            if (cantripIds.size() > maxCantrips) {
                throw new BadRequestException("Too many cantrips: max " + maxCantrips + ", got " + cantripIds.size());
            }
            validateSpellsForClass(cantripIds, charClass, 0);
        }

        if (spellIds != null && !spellIds.isEmpty()) {
            int maxSpells = getMaxKnownSpells(charClass.getName(), level);
            if (spellIds.size() > maxSpells) {
                throw new BadRequestException("Too many spells: max " + maxSpells + ", got " + spellIds.size());
            }
            int maxSpellLevel = getMaxSpellLevel(level, Boolean.TRUE.equals(charClass.getIsHalfCaster()));
            for (UUID spellId : spellIds) {
                Spell spell = spellRepository.findById(spellId)
                        .orElseThrow(() -> new BadRequestException("Spell not found: " + spellId));
                if (spell.getLevel() > maxSpellLevel) {
                    throw new BadRequestException("Spell '" + spell.getName() + "' (level " + spell.getLevel()
                            + ") exceeds max spell level " + maxSpellLevel);
                }
                if (spell.getLevel() == 0) {
                    throw new BadRequestException("Cantrips should be in cantripIds, not spellIds");
                }
            }
            validateSpellsForClass(spellIds, charClass, null);
        }
    }

    private void validateSpellsForClass(List<UUID> spellIds, CharacterClass charClass, Integer expectedLevel) {
        String classIdStr = charClass.getId().toString();
        for (UUID spellId : spellIds) {
            Spell spell = spellRepository.findById(spellId)
                    .orElseThrow(() -> new BadRequestException("Spell not found: " + spellId));
            if (spell.getAvailableToClassIdsJson() == null || !spell.getAvailableToClassIdsJson().contains(classIdStr)) {
                throw new BadRequestException("Spell '" + spell.getName() + "' is not available for class '" + charClass.getName() + "'");
            }
            if (expectedLevel != null && !spell.getLevel().equals(expectedLevel)) {
                throw new BadRequestException("Expected spell level " + expectedLevel + " but got " + spell.getLevel());
            }
        }
    }

    // --- Spell limit calculations (simplified 5e SRD) ---

    private int getMaxCantrips(String className, int level) {
        return switch (className) {
            case "Bard", "Sorcerer", "Warlock" -> level >= 10 ? 5 : level >= 4 ? 4 : 2;
            case "Cleric", "Druid", "Wizard" -> level >= 10 ? 5 : level >= 4 ? 4 : 3;
            default -> 0;
        };
    }

    private int getMaxKnownSpells(String className, int level) {
        return switch (className) {
            case "Bard" -> level + 3;
            case "Ranger" -> Math.max(0, (level + 1) / 2 + 1);
            case "Sorcerer" -> level + 1;
            case "Warlock" -> level + 1;
            case "Wizard" -> 6 + (level - 1) * 2;
            case "Paladin" -> Math.max(1, level / 2 + getAbilityModifier(14));
            case "Cleric", "Druid" -> level + 5;
            default -> 0;
        };
    }

    private int getMaxSpellLevel(int characterLevel, boolean isHalfCaster) {
        if (isHalfCaster) {
            return Math.min(5, (characterLevel + 1) / 2);
        }
        return Math.min(9, (characterLevel + 1) / 2);
    }

    // --- Stat helpers ---

    private Map<String, Integer> getRacialBonuses(CharacterRace race, UUID subraceId) {
        Map<String, Integer> bonuses = new HashMap<>();
        if (race.getAbilityScoreBonusesJson() != null) {
            try {
                var list = objectMapper.readValue(race.getAbilityScoreBonusesJson(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (var b : list) {
                    String ability = (String) b.get("ability");
                    int value = ((Number) b.get("value")).intValue();
                    bonuses.merge(ability, value, Integer::sum);
                }
            } catch (Exception ignored) {}
        }
        return bonuses;
    }

    private int getFinalStatValue(Map<UUID, Integer> baseScores, Map<String, Integer> racialBonuses,
                                   Map<String, StatType> statByName, String statName) {
        StatType st = statByName.get(statName);
        if (st == null) return 10;
        int base = baseScores.getOrDefault(st.getId(), 10);
        int racial = racialBonuses.getOrDefault(statName, 0);
        return base + racial;
    }

    private int getAbilityModifier(int score) {
        return (score - 10) / 2;
    }

    private int getWalkSpeed(CharacterRace race) {
        if (race.getSpeedJson() != null) {
            try {
                var map = objectMapper.readValue(race.getSpeedJson(), new TypeReference<Map<String, Integer>>() {});
                return map.getOrDefault("walk", 30);
            } catch (Exception ignored) {}
        }
        return 30;
    }

    private int calculateMaxHp(int hitDie, int level, int conModifier) {
        int firstLevel = hitDie + conModifier;
        int avgPerLevel = (hitDie / 2) + 1 + conModifier;
        return firstLevel + avgPerLevel * (level - 1);
    }

    private int getProficiencyBonus(int level) {
        if (level <= 4) return 2;
        if (level <= 8) return 3;
        if (level <= 12) return 4;
        if (level <= 16) return 5;
        return 6;
    }

    // --- Skill saving ---

    private void saveSkillProficiencies(PlayerCharacter character, List<UUID> chosenClassSkillIds, Background background) {
        List<String> bgSkillNames = referenceDataService.parseJsonStringList(background.getSkillProficiencyIdsJson());
        for (String skillName : bgSkillNames) {
            ProficiencySkill ps = proficiencySkillRepository.findByName(skillName).orElse(null);
            if (ps != null) {
                CharacterSkillProficiency csp = CharacterSkillProficiency.builder()
                        .character(character)
                        .skill(ps)
                        .source(SkillProficiencySource.BACKGROUND)
                        .build();
                skillProficiencyRepository.save(csp);
                character.getSkillProficiencies().add(csp);
            }
        }

        for (UUID skillId : chosenClassSkillIds) {
            ProficiencySkill ps = proficiencySkillRepository.findById(skillId)
                    .orElseThrow(() -> new BadRequestException("Skill not found: " + skillId));
            CharacterSkillProficiency csp = CharacterSkillProficiency.builder()
                    .character(character)
                    .skill(ps)
                    .source(SkillProficiencySource.CLASS)
                    .build();
            skillProficiencyRepository.save(csp);
            character.getSkillProficiencies().add(csp);
        }
    }

    private void saveKnownSpells(PlayerCharacter character, List<UUID> spellIds) {
        for (UUID spellId : spellIds) {
            Spell spell = spellRepository.findById(spellId)
                    .orElseThrow(() -> new BadRequestException("Spell not found: " + spellId));
            CharacterKnownSpell cks = CharacterKnownSpell.builder()
                    .character(character)
                    .spell(spell)
                    .build();
            knownSpellRepository.save(cks);
            character.getKnownSpells().add(cks);
        }
    }

    // --- JSON helpers ---

    private String serializeBiography(CreateFullCharacterRequest.BiographyDto bio) {
        if (bio == null) return null;
        try {
            return objectMapper.writeValueAsString(bio);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private BiographyResponse parseBiography(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, BiographyResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<CharacterAttackResponse> parseAttacks(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
