package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterKnownSpell;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.CharacterRewardAbilityScoreSelection;
import com.dnd.app.domain.content.CharacterRewardAbilityScoreSelectionId;
import com.dnd.app.domain.content.CharacterRewardSelection;
import com.dnd.app.domain.content.CharacterRewardSkillSelection;
import com.dnd.app.domain.content.CharacterRewardSkillSelectionId;
import com.dnd.app.domain.content.CharacterRewardSpellSelection;
import com.dnd.app.domain.content.CharacterRewardSpellSelectionId;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.SkillProficiencyLevel;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.dto.content.LevelUpRequest;
import com.dnd.app.dto.content.LevelUpResultResponse;
import com.dnd.app.dto.content.grant.GrantType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.CharacterRewardAbilityScoreSelectionRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.CharacterRewardSkillSelectionRepository;
import com.dnd.app.repository.CharacterRewardSpellSelectionRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.AbilityScores;
import com.dnd.app.util.Localization;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Final level-up COMMIT on the new content model (Phase 7). Validates the requested
 * reward-group selections, persists them to the final reward-selection model
 * (character_reward_selection + ability/skill/spell child selections), applies
 * deterministic grants, and returns manual action items for non-deterministic ones.
 *
 * <p>Writes only to the new reward-selection model — it never touches the legacy
 * character_acquired_rewards path. Runs in parallel with the legacy LevelUpService.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelUpCommandService {

    private static final int MAX_LEVEL = 20;

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ContentCharacterClassRepository contentClassRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ClassLevelRewardGroupRepository rewardGroupRepository;
    private final CharacterRewardSelectionRepository rewardSelectionRepository;
    private final CharacterRewardAbilityScoreSelectionRepository abilityScoreSelectionRepository;
    private final CharacterRewardSkillSelectionRepository skillSelectionRepository;
    private final CharacterRewardSpellSelectionRepository spellSelectionRepository;
    private final ClassLevelRewardGrantAbilityScoreRepository abilityGrantRepository;
    private final ClassLevelRewardGrantSkillProficiencyRepository skillGrantRepository;
    private final ClassLevelRewardGrantSpellRepository spellGrantRepository;
    private final CharacterStatRepository characterStatRepository;
    private final StatTypeRepository statTypeRepository;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final SpellRepository spellRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final LevelThresholdService thresholdService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public LevelUpResultResponse commitLevelUp(UUID characterId, String username, LevelUpRequest request, String lang) {
        String resolvedLang = Localization.normalize(lang);

        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        enforceWriteAccess(character, user);

        if (character.getTotalLevel() >= MAX_LEVEL) {
            throw new BadRequestException("Персонаж уже достиг максимального уровня");
        }
        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Персонаж еще не готов к повышению уровня");
        }

        ContentCharacterClass targetClass = contentClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));

        Optional<CharacterClassLevel> existingLevel =
                classLevelRepository.findByCharacterIdAndClassId(characterId, targetClass.getId());
        int currentClassLevel = existingLevel.map(CharacterClassLevel::getClassLevel).orElse(0);
        int newClassLevel = currentClassLevel + 1;
        if (newClassLevel > MAX_LEVEL) {
            throw new UnprocessableEntityException("Уровень класса не может быть выше 20");
        }
        if (currentClassLevel == 0 && character.getCampaign() != null) {
            enforceClassVisibleInCampaign(targetClass, character.getCampaign().getId());
        }

        List<ClassLevelRewardGroup> groups = rewardGroupRepository
                .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(targetClass.getId(), newClassLevel);
        Map<UUID, ClassLevelRewardGroup> groupById = groups.stream()
                .collect(Collectors.toMap(ClassLevelRewardGroup::getId, g -> g));

        Map<UUID, LevelUpRequest.GroupSelection> selectionByGroup = new LinkedHashMap<>();
        if (request.getSelections() != null) {
            for (LevelUpRequest.GroupSelection sel : request.getSelections()) {
                if (sel.getRewardGroupId() == null || !groupById.containsKey(sel.getRewardGroupId())) {
                    throw new UnprocessableEntityException(
                            "Группа наград не относится к этому классу и уровню: " + sel.getRewardGroupId());
                }
                selectionByGroup.put(sel.getRewardGroupId(), sel);
            }
        }

        List<LevelUpResultResponse.AppliedGrant> applied = new ArrayList<>();
        List<LevelUpResultResponse.ManualActionItem> manual = new ArrayList<>();

        int maxSpellLevel = maxSpellLevelFor(targetClass, newClassLevel);
        applyRewardGroups(character, groups, selectionByGroup, applied, manual, maxSpellLevel);

        // --- class level / total level / HP / proficiency ---
        if (existingLevel.isPresent()) {
            CharacterClassLevel ccl = existingLevel.get();
            ccl.setClassLevel(newClassLevel);
            classLevelRepository.save(ccl);
        } else {
            classLevelRepository.save(CharacterClassLevel.builder()
                    .characterId(characterId).classId(targetClass.getId()).classLevel(1).build());
        }

        int totalLevelBefore = character.getTotalLevel();
        character.setTotalLevel(totalLevelBefore + 1);

        int hitDie = targetClass.getHitDie() != null ? targetClass.getHitDie() : 8;
        int hpIncrease = Math.max(1, (hitDie / 2) + 1 + constitutionModifier(character));
        if (character.getMaxHp() != null) {
            character.setMaxHp(character.getMaxHp() + hpIncrease);
            int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
            character.setCurrentHp(currentHp + hpIncrease);
        }
        characterRepository.save(character);

        log.info("Content level-up committed: characterId={}, classId={}, newClassLevel={}, newTotalLevel={}, "
                        + "applied={}, manual={}, user={}",
                characterId, targetClass.getId(), newClassLevel, character.getTotalLevel(),
                applied.size(), manual.size(), username);

        return LevelUpResultResponse.builder()
                .newTotalLevel(character.getTotalLevel())
                .classLeveled(Localization.pick(resolvedLang, targetClass.getNameRu(), targetClass.getNameEn(),
                        targetClass.getNameEn() != null ? targetClass.getNameEn() : targetClass.getNameRu()))
                .newClassLevel(newClassLevel)
                .hpIncrease(hpIncrease)
                .newMaxHp(character.getMaxHp())
                .proficiencyBonusBefore(proficiencyBonus(totalLevelBefore))
                .proficiencyBonusAfter(proficiencyBonus(character.getTotalLevel()))
                .appliedGrants(applied)
                .manualActions(manual)
                .build();
    }

    /**
     * Applies and persists selections for level-1 reward groups during character creation.
     * This uses the final reward-selection model and intentionally does not change class
     * level, total level, HP, XP, or proficiency.
     */
    public LevelUpResultResponse applyInitialRewardSelections(
            PlayerCharacter character,
            ContentCharacterClass targetClass,
            List<LevelUpRequest.GroupSelection> selections,
            String lang) {
        String resolvedLang = Localization.normalize(lang);

        List<ClassLevelRewardGroup> groups = rewardGroupRepository
                .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(targetClass.getId(), 1);
        Map<UUID, ClassLevelRewardGroup> groupById = groups.stream()
                .collect(Collectors.toMap(ClassLevelRewardGroup::getId, g -> g));

        Map<UUID, LevelUpRequest.GroupSelection> selectionByGroup = new LinkedHashMap<>();
        if (selections != null) {
            for (LevelUpRequest.GroupSelection sel : selections) {
                if (sel.getRewardGroupId() == null || !groupById.containsKey(sel.getRewardGroupId())) {
                    throw new UnprocessableEntityException(
                            "Группа наград не относится к этому классу и 1 уровню: "
                                    + sel.getRewardGroupId());
                }
                if (selectionByGroup.put(sel.getRewardGroupId(), sel) != null) {
                    throw new UnprocessableEntityException(
                            "Дублирующаяся группа наград: " + sel.getRewardGroupId());
                }
            }
        }

        List<LevelUpResultResponse.AppliedGrant> applied = new ArrayList<>();
        List<LevelUpResultResponse.ManualActionItem> manual = new ArrayList<>();

        int maxSpellLevel = maxSpellLevelFor(targetClass, 1);
        applyRewardGroups(character, groups, selectionByGroup, applied, manual, maxSpellLevel);

        return LevelUpResultResponse.builder()
                .newTotalLevel(character.getTotalLevel())
                .classLeveled(Localization.pick(resolvedLang, targetClass.getNameRu(), targetClass.getNameEn(),
                        targetClass.getNameEn() != null ? targetClass.getNameEn() : targetClass.getNameRu()))
                .newClassLevel(1)
                .hpIncrease(0)
                .newMaxHp(character.getMaxHp())
                .proficiencyBonusBefore(proficiencyBonus(character.getTotalLevel()))
                .proficiencyBonusAfter(proficiencyBonus(character.getTotalLevel()))
                .appliedGrants(applied)
                .manualActions(manual)
                .build();
    }

    /**
     * Applies all reward groups for a single class level: CHOICE groups consume the player's
     * selection, while AUTO (non-CHOICE) groups apply their deterministic grants automatically.
     * Shared by the regular level-up commit and the level-1 character-creation path so both apply
     * auto grants identically.
     */
    private void applyRewardGroups(PlayerCharacter character,
                                   List<ClassLevelRewardGroup> groups,
                                   Map<UUID, LevelUpRequest.GroupSelection> selectionByGroup,
                                   List<LevelUpResultResponse.AppliedGrant> applied,
                                   List<LevelUpResultResponse.ManualActionItem> manual,
                                   int maxSpellLevel) {
        for (ClassLevelRewardGroup group : groups) {
            LevelUpRequest.GroupSelection sel = selectionByGroup.get(group.getId());
            if ("CHOICE".equalsIgnoreCase(group.getGroupKind())) {
                processChoiceGroup(character, group, sel, applied, manual, maxSpellLevel);
            } else {
                for (ClassLevelRewardGrant grant : group.getGrants()) {
                    applyGrant(character, null, grant, null, applied, manual, maxSpellLevel);
                }
            }
        }
    }

    private void processChoiceGroup(PlayerCharacter character, ClassLevelRewardGroup group,
                                    LevelUpRequest.GroupSelection sel,
                                    List<LevelUpResultResponse.AppliedGrant> applied,
                                    List<LevelUpResultResponse.ManualActionItem> manual,
                                    int maxSpellLevel) {
        List<UUID> optionIds = sel != null && sel.getOptionIds() != null ? sel.getOptionIds() : List.of();
        int n = optionIds.size();
        if (n < group.getChooseMin()) {
            throw new UnprocessableEntityException(
                    "Группа '" + group.getId() + "': выбрано меньше минимума (" + group.getChooseMin() + ")");
        }
        if (n > group.getChooseMax()) {
            throw new UnprocessableEntityException(
                    "Группа '" + group.getId() + "': выбрано больше максимума (" + group.getChooseMax() + ")");
        }
        if (new HashSet<>(optionIds).size() != n) {
            throw new UnprocessableEntityException("Группа '" + group.getId() + "': дублирующиеся опции");
        }

        Map<UUID, ClassLevelRewardOption> optionById = group.getOptions().stream()
                .collect(Collectors.toMap(ClassLevelRewardOption::getId, o -> o));

        for (UUID optionId : optionIds) {
            ClassLevelRewardOption option = optionById.get(optionId);
            if (option == null) {
                throw new UnprocessableEntityException(
                        "Опция " + optionId + " не относится к группе " + group.getId());
            }
            if (!Boolean.TRUE.equals(group.getRepeatable())
                    && rewardSelectionRepository.findByCharacterIdAndRewardOptionId(character.getId(), optionId).isPresent()) {
                throw new DuplicateResourceException("Опция уже выбрана ранее: " + optionId);
            }

            CharacterRewardSelection selection = rewardSelectionRepository.save(CharacterRewardSelection.builder()
                    .character(character)
                    .rewardGroup(group)
                    .rewardOption(option)
                    .build());

            for (ClassLevelRewardGrant grant : option.getGrants()) {
                applyGrant(character, selection,
                        grant, sel != null ? sel.getChildSelections() : null, applied, manual, maxSpellLevel);
            }
        }
    }

    private void applyGrant(PlayerCharacter character, CharacterRewardSelection selection,
                            ClassLevelRewardGrant grant, LevelUpRequest.ChildSelections child,
                            List<LevelUpResultResponse.AppliedGrant> applied,
                            List<LevelUpResultResponse.ManualActionItem> manual,
                            int maxSpellLevel) {
        GrantType type = GrantType.fromTextOrCustom(grant.getGrantType());
        switch (type) {
            case FEATURE -> applied.add(appliedGrant(grant, "Класс-фича получена"));
            case SUBCLASS -> applied.add(appliedGrant(grant, "Подкласс выбран"));
            case ABILITY_SCORE -> applyAbilityScore(character, selection, grant, child, applied);
            case SKILL_PROFICIENCY -> applySkill(character, selection, grant, child, applied);
            case SPELL -> applySpell(character, selection, grant, child, applied, maxSpellLevel);
            case FEAT -> manual.add(manualItem(grant, "Выберите/получите черту вручную"));
            case NUMERIC_MODIFIER -> manual.add(manualItem(grant, "Примените числовой модификатор вручную"));
            case CUSTOM_TEXT -> manual.add(manualItem(grant, "Ручной грант — заполните на листе персонажа"));
        }
    }

    private void applyAbilityScore(PlayerCharacter character, CharacterRewardSelection selection,
                                   ClassLevelRewardGrant grant, LevelUpRequest.ChildSelections child,
                                   List<LevelUpResultResponse.AppliedGrant> applied) {
        ClassLevelRewardGrantAbilityScore cfg = abilityGrantRepository.findById(grant.getId())
                .orElseThrow(() -> new UnprocessableEntityException("Нет конфигурации ABILITY_SCORE гранта"));
        List<LevelUpRequest.AbilityScoreChoice> choices =
                child != null && child.getAbilityScores() != null ? child.getAbilityScores() : List.of();

        int chooseCount = cfg.getChooseCount() != null ? cfg.getChooseCount() : 1;
        int bonusPerChoice = cfg.getBonusPerChoice() != null ? cfg.getBonusPerChoice() : 1;
        int maxScore = cfg.getMaxScore() != null ? cfg.getMaxScore() : 20;

        if (choices.size() != chooseCount) {
            throw new UnprocessableEntityException(
                    "ASI: нужно выбрать ровно " + chooseCount + " характеристик");
        }
        if (choices.stream().map(LevelUpRequest.AbilityScoreChoice::getAbilityScoreId).distinct().count()
                != choices.size()) {
            throw new UnprocessableEntityException("ASI: характеристики должны быть разными");
        }
        Set<UUID> allowed = cfg.getAbilityOptions() == null ? Set.of()
                : cfg.getAbilityOptions().stream().map(StatType::getId).collect(Collectors.toSet());

        for (LevelUpRequest.AbilityScoreChoice choice : choices) {
            if (choice.getAmount() == null || choice.getAmount() != bonusPerChoice) {
                throw new UnprocessableEntityException("ASI: прибавка должна быть " + bonusPerChoice);
            }
            if (!allowed.isEmpty() && !allowed.contains(choice.getAbilityScoreId())) {
                throw new UnprocessableEntityException(
                        "ASI: характеристика недоступна для этого гранта: " + choice.getAbilityScoreId());
            }
            CharacterStat stat = character.getStats().stream()
                    .filter(s -> s.getStatType() != null && s.getStatType().getId().equals(choice.getAbilityScoreId()))
                    .findFirst()
                    .orElseThrow(() -> new UnprocessableEntityException(
                            "Характеристика не принадлежит персонажу: " + choice.getAbilityScoreId()));
            int after = stat.getValue() + choice.getAmount();
            if (after > maxScore) {
                throw new UnprocessableEntityException(
                        "Характеристика '" + stat.getStatType().getNameRu() + "' не может превысить " + maxScore);
            }
            stat.setValue(after);
            characterStatRepository.save(stat);

            if (selection != null) {
                abilityScoreSelectionRepository.save(CharacterRewardAbilityScoreSelection.builder()
                        .id(new CharacterRewardAbilityScoreSelectionId(
                                selection.getId(), cfg.getId(), stat.getStatType().getId()))
                        .selection(selection).grant(cfg).abilityScore(stat.getStatType())
                        .bonusAmount(choice.getAmount()).build());
            }
            applied.add(appliedGrant(grant, "+" + choice.getAmount() + " "
                    + stat.getStatType().getNameRu() + " (=" + after + ")"));
        }
    }

    private void applySkill(PlayerCharacter character, CharacterRewardSelection selection,
                            ClassLevelRewardGrant grant, LevelUpRequest.ChildSelections child,
                            List<LevelUpResultResponse.AppliedGrant> applied) {
        ClassLevelRewardGrantSkillProficiency cfg = skillGrantRepository.findById(grant.getId())
                .orElseThrow(() -> new UnprocessableEntityException("Нет конфигурации SKILL_PROFICIENCY гранта"));
        List<UUID> skillIds = child != null && child.getSkillIds() != null ? child.getSkillIds() : List.of();

        int chooseCount = cfg.getChooseCount() != null ? cfg.getChooseCount() : 1;
        if (skillIds.size() != chooseCount) {
            throw new UnprocessableEntityException("Навыки: нужно выбрать ровно " + chooseCount);
        }
        if (new HashSet<>(skillIds).size() != skillIds.size()) {
            throw new UnprocessableEntityException("Навыки: дублирующиеся значения");
        }
        boolean any = Boolean.TRUE.equals(cfg.getAnySkill());
        boolean expertise = Boolean.TRUE.equals(cfg.getGrantsExpertise());
        Set<UUID> allowed = cfg.getSkillOptions() == null ? Set.of()
                : cfg.getSkillOptions().stream().map(ContentSkill::getId).collect(Collectors.toSet());

        for (UUID skillId : skillIds) {
            if (!any && !allowed.contains(skillId)) {
                throw new UnprocessableEntityException("Навык недоступен для этого гранта: " + skillId);
            }
            if (selection != null) {
                skillSelectionRepository.save(CharacterRewardSkillSelection.builder()
                        .id(new CharacterRewardSkillSelectionId(selection.getId(), cfg.getId(), skillId))
                        .selection(selection).grant(cfg)
                        .skill(entityManager.getReference(ContentSkill.class, skillId)).build());
            }
            if (expertise) {
                applyExpertise(character, skillId, grant, applied);
            } else {
                applyProficiency(character, skillId, grant, applied);
            }
        }
    }

    private void applyProficiency(PlayerCharacter character, UUID skillId,
                                  ClassLevelRewardGrant grant,
                                  List<LevelUpResultResponse.AppliedGrant> applied) {
        Optional<CharacterSkillProficiency> existing =
                skillProficiencyRepository.findByCharacterIdAndSkillId(character.getId(), skillId);
        if (existing.isPresent()) {
            // Already proficient (or has expertise) — nothing to add, just report.
            applied.add(appliedGrant(grant, "Навык уже освоен — пропущено"));
            return;
        }
        skillProficiencyRepository.save(CharacterSkillProficiency.builder()
                .character(character)
                .skill(entityManager.getReference(ContentSkill.class, skillId))
                .source(SkillProficiencySource.CLASS)
                .proficiencyLevel(SkillProficiencyLevel.PROFICIENT).build());
        applied.add(appliedGrant(grant, "Владение навыком получено"));
    }

    private void applyExpertise(PlayerCharacter character, UUID skillId,
                                ClassLevelRewardGrant grant,
                                List<LevelUpResultResponse.AppliedGrant> applied) {
        CharacterSkillProficiency row = skillProficiencyRepository
                .findByCharacterIdAndSkillId(character.getId(), skillId)
                .orElseThrow(() -> new UnprocessableEntityException(
                        "Нельзя получить Экспертность в навыке, которым вы не владеете"));
        if (row.getProficiencyLevel() == SkillProficiencyLevel.EXPERTISE) {
            throw new UnprocessableEntityException(
                    "Нельзя получить Экспертность в одном навыке дважды");
        }
        row.setProficiencyLevel(SkillProficiencyLevel.EXPERTISE);
        skillProficiencyRepository.save(row);
        applied.add(appliedGrant(grant, "Экспертность в навыке получена"));
    }

    private void applySpell(PlayerCharacter character, CharacterRewardSelection selection,
                            ClassLevelRewardGrant grant, LevelUpRequest.ChildSelections child,
                            List<LevelUpResultResponse.AppliedGrant> applied, int maxSpellLevel) {
        ClassLevelRewardGrantSpell cfg = spellGrantRepository.findById(grant.getId())
                .orElseThrow(() -> new UnprocessableEntityException("Нет конфигурации SPELL гранта"));
        List<UUID> spellIds = child != null && child.getSpellIds() != null ? child.getSpellIds() : List.of();

        int chooseCount = cfg.getChooseCount() != null ? cfg.getChooseCount() : 1;
        if (spellIds.size() != chooseCount) {
            throw new UnprocessableEntityException("Заклинания: нужно выбрать ровно " + chooseCount);
        }
        if (new HashSet<>(spellIds).size() != spellIds.size()) {
            throw new UnprocessableEntityException("Заклинания: дублирующиеся значения");
        }
        for (UUID spellId : spellIds) {
            Spell spell = spellRepository.findById(spellId)
                    .orElseThrow(() -> new UnprocessableEntityException("Заклинание не найдено"));
            validateSpellSelectable(spell, cfg, character, maxSpellLevel);
            if (selection != null) {
                spellSelectionRepository.save(CharacterRewardSpellSelection.builder()
                        .id(new CharacterRewardSpellSelectionId(selection.getId(), cfg.getId(), spellId))
                        .selection(selection).grant(cfg)
                        .spell(spell).build());
            }
            knownSpellRepository.save(CharacterKnownSpell.builder()
                    .character(character)
                    .spell(spell).build());
            applied.add(appliedGrant(grant, "Заклинание изучено"));
        }
    }

    /**
     * Guards client-supplied spell IDs at level-up: the spell must be visible in the character's
     * campaign and must satisfy the grant's authored filters (fixed spell / spell level / school).
     * The grant filters are only enforced when the author set them, so sparse seed data never
     * blocks a legitimate choice.
     */
    private void validateSpellSelectable(Spell spell, ClassLevelRewardGrantSpell cfg,
                                         PlayerCharacter character, int maxSpellLevel) {
        // A character can never learn a spell of a higher circle than their class level grants
        // access to. Cantrips (level 0) are always allowed. This mirrors the cap enforced at
        // character creation in ContentCharacterCreationService#getMaxSpellLevel.
        if (spell.getLevel() != null && spell.getLevel() > 0 && spell.getLevel() > maxSpellLevel) {
            throw new BadRequestException("Уровень заклинания (" + spell.getLevel()
                    + ") превышает максимально доступный на этом уровне класса (" + maxSpellLevel + ")");
        }
        UUID campaignId = character.getCampaign() != null ? character.getCampaign().getId() : null;
        if (spell.getHomebrew() != null) {
            if (campaignId == null) {
                throw new BadRequestException("Заклинание недоступно для персонажа вне кампании");
            }
            Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
            if (!pkgIds.contains(spell.getHomebrew().getId())) {
                throw new BadRequestException("Заклинание недоступно в текущей кампании");
            }
        }
        if (cfg.getSpell() != null && !cfg.getSpell().getId().equals(spell.getId())) {
            throw new BadRequestException("Это заклинание не предусмотрено выбранным грантом");
        }
        if (cfg.getSpellLevel() != null && spell.getLevel() != null
                && !cfg.getSpellLevel().equals(spell.getLevel())) {
            throw new BadRequestException("Уровень заклинания не соответствует гранту");
        }
        if (cfg.getSchool() != null && spell.getSchool() != null
                && !cfg.getSchool().getId().equals(spell.getSchool().getId())) {
            throw new BadRequestException("Школа заклинания не соответствует гранту");
        }
    }

    /**
     * Highest spell circle a spell grant may hand out at this class level. Non-spellcasting
     * classes get no cap ({@link Integer#MAX_VALUE}) since they should not carry spell grants.
     */
    private int maxSpellLevelFor(ContentCharacterClass charClass, int classLevel) {
        if (!Boolean.TRUE.equals(charClass.getSpellcaster())) {
            return Integer.MAX_VALUE;
        }
        return getMaxSpellLevel(classLevel, Boolean.TRUE.equals(charClass.getHalfCaster()));
    }

    private int getMaxSpellLevel(int level, boolean halfCaster) {
        return halfCaster ? Math.min(5, (level + 1) / 2) : Math.min(9, (level + 1) / 2);
    }

    private LevelUpResultResponse.AppliedGrant appliedGrant(ClassLevelRewardGrant grant, String summary) {
        return LevelUpResultResponse.AppliedGrant.builder()
                .grantId(grant.getId()).grantType(grant.getGrantType()).summary(summary).build();
    }

    private LevelUpResultResponse.ManualActionItem manualItem(ClassLevelRewardGrant grant, String instruction) {
        return LevelUpResultResponse.ManualActionItem.builder()
                .grantId(grant.getId()).grantType(grant.getGrantType()).instruction(instruction).build();
    }

    private void enforceClassVisibleInCampaign(ContentCharacterClass clazz, UUID campaignId) {
        if (clazz.getHomebrew() == null) {
            return;
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(clazz.getHomebrew().getId())) {
            throw new BadRequestException("Этот класс недоступен в текущей кампании");
        }
    }

    private int constitutionModifier(PlayerCharacter character) {
        StatType con = statTypeRepository.findAll().stream()
                .filter(st -> "Constitution".equals(st.getNameEn()))
                .findFirst().orElse(null);
        if (con == null || character.getStats() == null) {
            return 0;
        }
        return character.getStats().stream()
                .filter(s -> s.getStatType() != null && s.getStatType().getId().equals(con.getId()))
                .findFirst()
                .map(CharacterStat::getValue)
                .map(AbilityScores::modifier)
                .orElse(0);
    }

    private int proficiencyBonus(int totalLevel) {
        int level = Math.max(1, totalLevel);
        return ((level - 1) / 4) + 2;
    }

    private void enforceWriteAccess(PlayerCharacter character, User user) {
        boolean isOwner = character.getOwner().getId().equals(user.getId());
        boolean isCampaignGM = character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!isOwner && !isCampaignGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на повышение уровня этого персонажа");
        }
    }
}
