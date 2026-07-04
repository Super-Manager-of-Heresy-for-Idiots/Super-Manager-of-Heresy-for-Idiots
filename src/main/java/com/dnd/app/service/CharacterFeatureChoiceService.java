package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.SkillProficiencyLevel;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.domain.featurerule.CharacterFeatureChoice;
import com.dnd.app.domain.featurerule.ChoiceOptionType;
import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import com.dnd.app.domain.featurerule.FeatureChoiceOption;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.featurerule.FeatureChoiceGroupResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterFeatureChoiceRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
import com.dnd.app.repository.FeatureChoiceOptionRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists and records a character's feature choices (Fighting Style, Expertise skills, Metamagic, …) — the
 * choice half of Stage 4 that was deferred (only concrete {@code targetId} skill grants auto-applied before).
 *
 * <p>Safe-by-default application: a {@code skill} option is materialised as a skill proficiency (the only
 * character-side proficiency store that exists today); every other option type is recorded and surfaced so
 * the UI can show it and later stages can apply its mechanical effect. All flag-gated: a hard no-op / empty
 * list unless {@code app.feature-rules.runtime-enabled}. Access is enforced by the caller (owner/GM/ADMIN).</p>
 */
@Service
@RequiredArgsConstructor
public class CharacterFeatureChoiceService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureChoiceGroupRepository choiceGroupRepository;
    private final FeatureChoiceOptionRepository choiceOptionRepository;
    private final CharacterFeatureChoiceRepository choiceRepository;
    private final PlayerCharacterRepository characterRepository;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final ContentSkillRepository contentSkillRepository;

    @Transactional(readOnly = true)
    public List<FeatureChoiceGroupResponse> list(UUID characterId) {
        List<FeatureRule> rules = approvedRules(characterId);
        if (rules.isEmpty()) {
            return List.of();
        }
        Map<UUID, UUID> ruleToFeature = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, FeatureRule::getOwnerId, (a, b) -> a));
        List<FeatureChoiceGroup> groups = choiceGroupRepository.findByFeatureRuleIdIn(ruleToFeature.keySet());
        if (groups.isEmpty()) {
            return List.of();
        }
        List<UUID> groupIds = groups.stream().map(FeatureChoiceGroup::getId).toList();
        Map<UUID, List<FeatureChoiceOption>> optionsByGroup = choiceOptionRepository.findByChoiceGroupIdIn(groupIds)
                .stream().collect(Collectors.groupingBy(FeatureChoiceOption::getChoiceGroupId));
        Map<UUID, List<CharacterFeatureChoice>> choicesByGroup = choiceRepository.findByCharacterId(characterId)
                .stream().collect(Collectors.groupingBy(CharacterFeatureChoice::getChoiceGroupId));

        List<FeatureChoiceGroupResponse> out = new ArrayList<>();
        for (FeatureChoiceGroup g : groups) {
            out.add(toResponse(g, ruleToFeature.get(g.getFeatureRuleId()),
                    optionsByGroup.getOrDefault(g.getId(), List.of()),
                    choicesByGroup.getOrDefault(g.getId(), List.of())));
        }
        return out;
    }

    @Transactional
    public FeatureChoiceGroupResponse choose(UUID characterId, UUID groupId, String optionType, UUID targetEntityId) {
        if (!flags.isRuntimeEnabled()) {
            throw new BadRequestException("Рантайм умений выключен");
        }
        FeatureChoiceGroup group = choiceGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа выбора не найдена"));
        FeatureRule rule = approvedRules(characterId).stream()
                .filter(r -> r.getId().equals(group.getFeatureRuleId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Этот выбор недоступен персонажу"));

        int max = maxChoices(group);
        long made = choiceRepository.countByCharacterIdAndChoiceGroupId(characterId, groupId);
        if (made >= max) {
            throw new BadRequestException("Все выборы в этой группе уже сделаны");
        }

        List<FeatureChoiceOption> options = choiceOptionRepository.findByChoiceGroupIdOrderBySortOrderAsc(groupId);
        boolean enumerated = options.stream().anyMatch(o -> o.getTargetEntityId() != null);
        if (enumerated && targetEntityId != null
                && options.stream().noneMatch(o -> targetEntityId.equals(o.getTargetEntityId()))) {
            throw new BadRequestException("Недопустимый вариант выбора");
        }
        boolean duplicate = choiceRepository.findByCharacterIdAndChoiceGroupId(characterId, groupId).stream()
                .anyMatch(c -> targetEntityId != null && targetEntityId.equals(c.getTargetEntityId()));
        if (duplicate) {
            throw new BadRequestException("Этот вариант уже выбран");
        }

        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        choiceRepository.save(CharacterFeatureChoice.builder()
                .characterId(characterId)
                .featureId(rule.getOwnerId())
                .choiceGroupId(groupId)
                .optionType(optionType)
                .targetEntityId(targetEntityId)
                .chosenAtLevel(character.getTotalLevel())
                .build());

        // Safe auto-application: only SKILL proficiency (the only character-side store that exists today).
        if (ChoiceOptionType.SKILL.getCode().equalsIgnoreCase(optionType) && targetEntityId != null) {
            applySkill(character, targetEntityId);
        }

        return toResponse(group, rule.getOwnerId(), options,
                choiceRepository.findByCharacterIdAndChoiceGroupId(characterId, groupId));
    }

    @Transactional
    public void unchoose(UUID characterId, UUID choiceId) {
        choiceRepository.findById(choiceId)
                .filter(c -> c.getCharacterId().equals(characterId))
                .ifPresent(c -> {
                    if (ChoiceOptionType.SKILL.getCode().equalsIgnoreCase(c.getOptionType())
                            && c.getTargetEntityId() != null) {
                        skillProficiencyRepository.findByCharacterIdAndSkillId(characterId, c.getTargetEntityId())
                                .filter(sp -> sp.getSource() == SkillProficiencySource.FEATURE)
                                .ifPresent(skillProficiencyRepository::delete);
                    }
                    choiceRepository.delete(c);
                });
    }

    private List<FeatureRule> approvedRules(UUID characterId) {
        if (!flags.isRuntimeEnabled()) {
            return List.of();
        }
        List<ClassFeature> features = resolver.knownBaseClassFeatures(characterId);
        return resolver.approvedEnabledRules(features.stream().map(ClassFeature::getId).toList());
    }

    /** maxChoicesFormulaId is not evaluated yet; default max == min (i.e. "choose exactly N"). */
    private int maxChoices(FeatureChoiceGroup g) {
        return g.getMinChoices() != null ? g.getMinChoices() : 1;
    }

    private void applySkill(PlayerCharacter character, UUID skillId) {
        if (skillProficiencyRepository.findByCharacterIdAndSkillId(character.getId(), skillId).isPresent()) {
            return;
        }
        ContentSkill skill = contentSkillRepository.findById(skillId).orElse(null);
        if (skill == null) {
            return;
        }
        skillProficiencyRepository.save(CharacterSkillProficiency.builder()
                .character(character)
                .skill(skill)
                .source(SkillProficiencySource.FEATURE)
                .proficiencyLevel(SkillProficiencyLevel.PROFICIENT)
                .build());
    }

    private FeatureChoiceGroupResponse toResponse(FeatureChoiceGroup g, UUID featureId,
            List<FeatureChoiceOption> options, List<CharacterFeatureChoice> selections) {
        int min = g.getMinChoices() != null ? g.getMinChoices() : 1;
        int max = maxChoices(g);
        int chosen = selections.size();
        return FeatureChoiceGroupResponse.builder()
                .groupId(g.getId())
                .featureId(featureId)
                .choiceKey(g.getChoiceKey())
                .minChoices(min)
                .maxChoices(max)
                .chosenCount(chosen)
                .remaining(Math.max(0, max - chosen))
                .options(options.stream()
                        .map(o -> FeatureChoiceGroupResponse.Option.builder()
                                .id(o.getId())
                                .optionType(o.getOptionType())
                                .targetEntityId(o.getTargetEntityId())
                                .filterRuleId(o.getFilterRuleId())
                                .build())
                        .toList())
                .selections(selections.stream()
                        .map(c -> FeatureChoiceGroupResponse.Selection.builder()
                                .id(c.getId())
                                .optionType(c.getOptionType())
                                .targetEntityId(c.getTargetEntityId())
                                .chosenAtLevel(c.getChosenAtLevel())
                                .build())
                        .toList())
                .build();
    }
}
