package com.dnd.app.service;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CharacterKnownSpell;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterWallet;
import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.enums.ScoreMethod;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.dto.content.ContentCharacterCreationResponse;
import com.dnd.app.dto.request.CreateContentCharacterRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.util.AbilityScores;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CampaignMemberRepository;
import com.dnd.app.repository.CampaignRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.CharacterWalletRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс ContentCharacterCreationService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCharacterCreationService {

    private static final Set<Integer> STANDARD_ARRAY = Set.of(15, 14, 13, 12, 10, 8);
    private static final Map<Integer, Integer> POINT_BUY_COST = Map.of(
            8, 0, 9, 1, 10, 2, 11, 3, 12, 4, 13, 5, 14, 7, 15, 9);

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ContentCharacterClassRepository contentClassRepository;
    private final ContentSkillRepository contentSkillRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final BackgroundRepository backgroundRepository;
    private final SpellRepository spellRepository;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final CharacterWalletRepository walletRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final SpeciesService speciesService;
    private final LevelUpCommandService levelUpCommandService;
    private final CharacterFeatureGrantService characterFeatureGrantService;
    private final CharacterFeatService characterFeatService;
    private final LevelThresholdService levelThresholdService;
    private final ObjectMapper objectMapper;

    /**
     * Создает результат операции "create campaign character" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Создает результат операции "create campaign character" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ContentCharacterCreationResponse createCampaignCharacter(
            UUID campaignId, CreateContentCharacterRequest req, String username) {
        User owner = loadUser(username);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        if (!campaignMemberRepository.existsByCampaignIdAndUserIdAndKickedFalse(campaign.getId(), owner.getId())) {
            throw new BadRequestException("You are not a member of this campaign");
        }

        ContentCharacterClass charClass = loadClassVisibleInCampaign(req.getClassId(), campaignId);
        Species species = speciesService.getSelectableSpecies(campaignId, req.getRaceId());
        return create(campaign, charClass, species, req, owner);
    }

    /**
     * Создает результат операции "create vanilla character" в рамках бизнес-логики домена.
     * @param req входящее значение req, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ContentCharacterCreationResponse createVanillaCharacter(
            CreateContentCharacterRequest req, String username) {
        User owner = loadUser(username);
        ContentCharacterClass charClass = contentClassRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        if (charClass.getHomebrew() != null) {
            throw new BadRequestException("Homebrew classes cannot be used in vanilla characters");
        }
        Species species = speciesService.getSelectableVanillaSpecies(req.getRaceId());
        return create(null, charClass, species, req, owner);
    }

    // --- core ---

    private ContentCharacterCreationResponse create(Campaign campaign, ContentCharacterClass charClass,
                                                    Species species, CreateContentCharacterRequest req, User owner) {
        // D&D 2024: no lineages/subraces — species carries size/speed/traits; ASI,
        // proficiencies and languages come from the Background below.

        Background background = backgroundRepository.findById(req.getBackgroundId())
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        if (campaign == null && background.getHomebrew() != null) {
            throw new BadRequestException("Homebrew backgrounds cannot be used in vanilla characters");
        }
        if (campaign != null) {
            requireVisibleInCampaign(background.getHomebrew(), campaign.getId(),
                    "Background is not available in this campaign");
        }

        int targetLevel = req.getLevel();
        if (targetLevel > 20) {
            throw new BadRequestException("Level must not exceed 20");
        }
        // A target level below 1 is treated as level 1: the character is created fresh
        // (level 1, XP for level 1) and the player levels them up themselves.
        if (targetLevel < 1) {
            targetLevel = 1;
        }

        // The character is always created at level 1. When a higher target level is
        // requested, we instead grant enough XP so the player can climb to that level
        // through the normal level-up mechanic (choosing features/ASIs/spells each step).
        int creationLevel = 1;
        long startingExperience = levelThresholdService.experienceForLevel(targetLevel);

        ScoreMethod scoreMethod = parseScoreMethod(req.getScoreMethod());
        validateAbilityScores(req.getAbilityScores(), scoreMethod);
        List<UUID> chosenSkills = validateSkillChoices(req.getChosenSkillIds(), charClass);
        validateSpells(req.getCantripIds(), req.getSpellIds(), charClass, creationLevel, campaign);

        // ability scores
        List<StatType> allStatTypes = statTypeRepository.findAll();
        Map<UUID, StatType> statById = allStatTypes.stream()
                .collect(Collectors.toMap(StatType::getId, s -> s));
        Map<String, StatType> statByName = allStatTypes.stream()
                .filter(s -> s.getNameEn() != null)
                .collect(Collectors.toMap(StatType::getNameEn, s -> s, (a, b) -> a));
        Map<UUID, Integer> baseScores = new java.util.HashMap<>();
        for (var entry : req.getAbilityScores()) {
            if (!statById.containsKey(entry.getStatId())) {
                throw new BadRequestException("Unknown ability score: " + entry.getStatId());
            }
            baseScores.put(entry.getStatId(), entry.getBaseValue());
        }

        int hitDie = charClass.getHitDie() != null ? charClass.getHitDie() : 8;
        int conModifier = abilityModifier(finalScore(baseScores, statByName, "Constitution"));
        int dexModifier = abilityModifier(finalScore(baseScores, statByName, "Dexterity"));
        int maxHp = calculateMaxHp(hitDie, creationLevel, conModifier);
        int armorClass = 10 + dexModifier;
        int walkSpeed = 30;

        PlayerCharacter character = PlayerCharacter.builder()
                .name(req.getName())
                .totalLevel(creationLevel)
                .experience(startingExperience)
                .race(species)
                .selectedLineageId(null)
                .raceSnapshotJson(speciesService.buildSpeciesSnapshotJson(species))
                .owner(owner)
                .campaign(campaign)
                .playerName(req.getPlayerName())
                .background(background)
                .armorClass(armorClass)
                .speed(walkSpeed)
                .maxHp(maxHp)
                .currentHp(maxHp)
                .hitDiceType("d" + hitDie)
                .hitDiceTotal(creationLevel + "d" + hitDie)
                .scoreMethod(scoreMethod)
                .proficiencies(req.getProficiencies())
                .equipment(req.getEquipment())
                .features(req.getFeatures())
                .alignment(req.getAlignment())
                .biographyJson(serializeOrNull(req.getBiography()))
                .attacksJson(serializeOrNull(req.getAttacks()))
                .build();
        character = characterRepository.saveAndFlush(character);

        // class level stores the NEW content class id (raw classId column; FK relaxed in 060)
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(character.getId())
                .classId(charClass.getId())
                .classLevel(creationLevel)
                .build();
        classLevelRepository.saveAndFlush(ccl);
        character.getClassLevels().add(ccl);

        // stats (ability_score)
        for (StatType st : allStatTypes) {
            int base = baseScores.getOrDefault(st.getId(), 10);
            CharacterStat stat = CharacterStat.builder()
                    .character(character)
                    .statType(st)
                    .value(base)
                    .build();
            characterStatRepository.save(stat);
            character.getStats().add(stat);
        }

        // class skill proficiencies reference the content skill (skill_id FK relaxed in 060)
        for (UUID skillId : chosenSkills) {
            ContentSkill skillRef = entityManager.getReference(ContentSkill.class, skillId);
            CharacterSkillProficiency csp = CharacterSkillProficiency.builder()
                    .character(character)
                    .skill(skillRef)
                    .source(SkillProficiencySource.CLASS)
                    .build();
            skillProficiencyRepository.save(csp);
            character.getSkillProficiencies().add(csp);
        }

        // known spells
        List<UUID> knownSpellIds = new ArrayList<>();
        knownSpellIds.addAll(saveKnownSpells(character, req.getCantripIds()));
        knownSpellIds.addAll(saveKnownSpells(character, req.getSpellIds()));

        // wallet (currency)
        if (req.getStartingCoins() != null) {
            for (var coin : req.getStartingCoins()) {
                CurrencyType ct = currencyTypeRepository.findById(coin.getCurrencyTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Currency type not found"));
                if (campaign == null && ct.getHomebrew() != null) {
                    throw new BadRequestException("Homebrew currency cannot be used in vanilla characters");
                }
                if (campaign != null) {
                    requireVisibleInCampaign(ct.getHomebrew(), campaign.getId(),
                            "Currency is not available in this campaign");
                }
                walletRepository.save(CharacterWallet.builder()
                        .character(character)
                        .currencyType(ct)
                        .amount(BigDecimal.valueOf(coin.getAmount()))
                        .build());
            }
        }

        levelUpCommandService.applyInitialRewardSelections(
                character, charClass, req.getInitialRewardSelections(), "ru");

        // S1 (polymorphic owner): materialise the background's static skill grants. Hard-gated inside
        // (no-op unless app.feature-rules.runtime-enabled), so the existing creation flow is unaffected.
        characterFeatureGrantService.applyForBackground(character, background.getId());

        // S1 (FEAT owner): a background may grant a fixed feat — record it structurally (also
        // auto-provisions any feat-bound resources). Idempotent and swallow-on-failure.
        if (background.getGrantedFeat() != null) {
            characterFeatService.grantFromSource(character, background.getGrantedFeat().getId(), "background");
        }

        log.info("Content character created: id={}, name='{}', classId={}, createdLevel={}, targetLevel={}, "
                        + "experience={}, owner={}, campaign={}",
                character.getId(), character.getName(), charClass.getId(), creationLevel, targetLevel,
                startingExperience, owner.getUsername(), campaign != null ? campaign.getId() : null);

        return ContentCharacterCreationResponse.builder()
                .id(character.getId())
                .name(character.getName())
                .classId(charClass.getId())
                .totalLevel(creationLevel)
                .campaignId(campaign != null ? campaign.getId() : null)
                .skillProficiencyIds(chosenSkills)
                .knownSpellIds(knownSpellIds)
                .build();
    }

    private List<UUID> saveKnownSpells(PlayerCharacter character, List<UUID> spellIds) {
        if (spellIds == null) {
            return List.of();
        }
        List<UUID> saved = new ArrayList<>();
        for (UUID spellId : spellIds) {
            Spell spell = spellRepository.findById(spellId)
                    .orElseThrow(() -> new BadRequestException("Spell not found: " + spellId));
            CharacterKnownSpell cks = CharacterKnownSpell.builder()
                    .character(character)
                    .spell(spell)
                    .build();
            knownSpellRepository.save(cks);
            character.getKnownSpells().add(cks);
            saved.add(spellId);
        }
        return saved;
    }

    /** Serializes a biography/attacks payload to JSON text, or null when absent/empty. */
    private String serializeOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BadRequestException("Failed to serialize character payload", e);
        }
    }

    // --- visibility ---

    private ContentCharacterClass loadClassVisibleInCampaign(UUID classId, UUID campaignId) {
        ContentCharacterClass charClass = contentClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        if (charClass.getHomebrew() != null) {
            Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
            if (!pkgIds.contains(charClass.getHomebrew().getId())) {
                throw new BadRequestException("Selected class is not available in this campaign");
            }
        }
        return charClass;
    }

    /** Homebrew content is usable in a campaign only if its package is attached to that campaign. */
    private void requireVisibleInCampaign(HomebrewPackage homebrew, UUID campaignId, String message) {
        if (homebrew == null) return;
        if (!campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId).contains(homebrew.getId())) {
            throw new BadRequestException(message);
        }
    }

    // --- validation ---

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ScoreMethod parseScoreMethod(String method) {
        try {
            return ScoreMethod.valueOf(method);
        } catch (Exception e) {
            throw new BadRequestException(
                    "Invalid score method: " + method + ". Must be STANDARD_ARRAY, POINT_BUY, or ROLL",
                    e);
        }
    }

    private void validateAbilityScores(List<CreateContentCharacterRequest.AbilityScoreEntry> scores, ScoreMethod method) {
        if (scores == null || scores.size() != 6) {
            throw new BadRequestException("Exactly 6 ability scores required");
        }
        Set<UUID> seen = new HashSet<>();
        for (var entry : scores) {
            if (!seen.add(entry.getStatId())) {
                throw new BadRequestException("Duplicate stat ID in ability scores");
            }
        }
        List<Integer> values = scores.stream()
                .map(CreateContentCharacterRequest.AbilityScoreEntry::getBaseValue).toList();
        switch (method) {
            case STANDARD_ARRAY -> {
                List<Integer> sorted = new ArrayList<>(values);
                java.util.Collections.sort(sorted);
                List<Integer> expected = new ArrayList<>(STANDARD_ARRAY);
                java.util.Collections.sort(expected);
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

    /** Validates chosen skills against class_skill_option and returns the de-duplicated list. */
    private List<UUID> validateSkillChoices(List<UUID> chosenIds, ContentCharacterClass charClass) {
        List<UUID> chosen = chosenIds != null ? chosenIds : List.of();
        int expected = charClass.getSkillChoiceCount() != null ? charClass.getSkillChoiceCount() : 0;
        if (chosen.size() != expected) {
            throw new BadRequestException("Expected " + expected + " skill choices, got " + chosen.size());
        }
        if (new HashSet<>(chosen).size() != chosen.size()) {
            throw new BadRequestException("Duplicate skill choice");
        }
        boolean any = Boolean.TRUE.equals(charClass.getSkillChoiceAny());
        Set<UUID> allowed = charClass.getSkillOptions().stream()
                .map(ContentSkill::getId).collect(Collectors.toSet());
        for (UUID skillId : chosen) {
            if (!contentSkillRepository.existsById(skillId)) {
                throw new BadRequestException("Skill not found: " + skillId);
            }
            if (!any && !allowed.contains(skillId)) {
                throw new BadRequestException("Skill " + skillId + " is not available for this class");
            }
        }
        return chosen;
    }

    private void validateSpells(List<UUID> cantripIds, List<UUID> spellIds,
                                ContentCharacterClass charClass, int level, Campaign campaign) {
        boolean hasCantrips = cantripIds != null && !cantripIds.isEmpty();
        boolean hasSpells = spellIds != null && !spellIds.isEmpty();
        boolean vanilla = campaign == null;

        if (!Boolean.TRUE.equals(charClass.getSpellcaster())) {
            if (hasCantrips || hasSpells) {
                throw new BadRequestException("Non-spellcaster class cannot have spells");
            }
            return;
        }

        Set<UUID> activePackageIds = vanilla ? Set.of()
                : campaignHomebrewRepository.findPackageIdsByCampaignId(campaign.getId());

        if (hasCantrips) {
            if (!Boolean.TRUE.equals(charClass.getHasCantrips())) {
                throw new BadRequestException("This class does not have cantrips");
            }
            for (UUID id : cantripIds) {
                Spell spell = spellRepository.findById(id)
                        .orElseThrow(() -> new BadRequestException("Spell not found: " + id));
                requireSpellVisible(spell, vanilla, activePackageIds);
                if (spell.getLevel() != null && spell.getLevel() != 0) {
                    throw new BadRequestException("Cantrips must be level 0 spells");
                }
            }
        }

        if (hasSpells) {
            int maxSpellLevel = getMaxSpellLevel(level, Boolean.TRUE.equals(charClass.getHalfCaster()));
            for (UUID id : spellIds) {
                Spell spell = spellRepository.findById(id)
                        .orElseThrow(() -> new BadRequestException("Spell not found: " + id));
                requireSpellVisible(spell, vanilla, activePackageIds);
                if (spell.getLevel() != null && spell.getLevel() == 0) {
                    throw new BadRequestException("Cantrips should be in cantripIds, not spellIds");
                }
                if (spell.getLevel() != null && spell.getLevel() > maxSpellLevel) {
                    throw new BadRequestException("Spell (level " + spell.getLevel()
                            + ") exceeds max spell level " + maxSpellLevel);
                }
            }
        }
    }

    private void requireSpellVisible(Spell spell, boolean vanilla, Set<UUID> activePackageIds) {
        if (spell.getHomebrew() == null) return;
        if (vanilla) {
            throw new BadRequestException("Homebrew spells cannot be used in vanilla characters");
        }
        if (!activePackageIds.contains(spell.getHomebrew().getId())) {
            throw new BadRequestException("Spell is not available in this campaign");
        }
    }

    private int getMaxSpellLevel(int level, boolean halfCaster) {
        return halfCaster ? Math.min(5, (level + 1) / 2) : Math.min(9, (level + 1) / 2);
    }

    private int finalScore(Map<UUID, Integer> baseScores, Map<String, StatType> statByName, String statName) {
        StatType st = statByName.get(statName);
        if (st == null) {
            return 10;
        }
        return baseScores.getOrDefault(st.getId(), 10);
    }

    private int abilityModifier(int score) {
        return AbilityScores.modifier(score);
    }

    private int calculateMaxHp(int hitDie, int level, int conModifier) {
        int firstLevel = hitDie + conModifier;
        int avgPerLevel = (hitDie / 2) + 1 + conModifier;
        return firstLevel + avgPerLevel * (level - 1);
    }
}
