package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.featurerule.CharacterFeatureChoice;
import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import com.dnd.app.domain.featurerule.FeatureChoiceOption;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.featurerule.FeatureChoiceGroupResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterFeatureChoiceRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
import com.dnd.app.repository.FeatureChoiceOptionRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterFeatureChoiceServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private CharacterFeatureResolver resolver;
    @Mock private FeatureChoiceGroupRepository choiceGroupRepository;
    @Mock private FeatureChoiceOptionRepository choiceOptionRepository;
    @Mock private CharacterFeatureChoiceRepository choiceRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private CharacterSkillProficiencyRepository skillProficiencyRepository;
    @Mock private ContentSkillRepository contentSkillRepository;

    @InjectMocks private CharacterFeatureChoiceService service;

    private final UUID charId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID ruleId = UUID.randomUUID();
    private final UUID featureId = UUID.randomUUID();
    private final UUID skillId = UUID.randomUUID();

    @Test
    void listIsEmptyWhenRuntimeDisabled() {
        when(flags.isRuntimeEnabled()).thenReturn(false);
        assertThat(service.list(charId)).isEmpty();
    }

    @Test
    void chooseRejectsWhenRuntimeDisabled() {
        when(flags.isRuntimeEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.choose(charId, groupId, "skill", skillId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void chooseRecordsSelectionAndAppliesSkill() {
        when(flags.isRuntimeEnabled()).thenReturn(true);
        when(choiceGroupRepository.findById(groupId)).thenReturn(Optional.of(
                FeatureChoiceGroup.builder().id(groupId).featureRuleId(ruleId).choiceKey("skill").minChoices(1).build()));
        when(resolver.knownBaseClassFeatures(charId)).thenReturn(List.of());
        when(resolver.approvedEnabledRules(anyCollection())).thenReturn(List.of(
                FeatureRule.builder().id(ruleId).ownerId(featureId).build()));
        when(choiceRepository.countByCharacterIdAndChoiceGroupId(charId, groupId)).thenReturn(0L);
        when(choiceOptionRepository.findByChoiceGroupIdOrderBySortOrderAsc(groupId)).thenReturn(List.of(
                FeatureChoiceOption.builder().id(UUID.randomUUID()).choiceGroupId(groupId)
                        .optionType("skill").targetEntityId(skillId).build()));
        when(choiceRepository.findByCharacterIdAndChoiceGroupId(charId, groupId))
                .thenReturn(List.<CharacterFeatureChoice>of());
        when(characterRepository.findById(charId)).thenReturn(Optional.of(
                PlayerCharacter.builder().id(charId).totalLevel(4).build()));
        when(skillProficiencyRepository.findByCharacterIdAndSkillId(charId, skillId)).thenReturn(Optional.empty());
        when(contentSkillRepository.findById(skillId)).thenReturn(Optional.of(new ContentSkill()));
        when(choiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(skillProficiencyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FeatureChoiceGroupResponse res = service.choose(charId, groupId, "skill", skillId);

        assertThat(res.getGroupId()).isEqualTo(groupId);
        assertThat(res.getFeatureId()).isEqualTo(featureId);
        verify(choiceRepository, times(1)).save(any(CharacterFeatureChoice.class));
        verify(skillProficiencyRepository, times(1)).save(any(CharacterSkillProficiency.class));
    }
}
