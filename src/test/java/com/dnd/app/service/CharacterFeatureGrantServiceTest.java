package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.SkillProficiencyLevel;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterFeatureGrantServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private ClassFeatureRepository classFeatureRepository;
    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureProficiencyGrantRepository proficiencyGrantRepository;
    @Mock private CharacterSkillProficiencyRepository skillProficiencyRepository;
    @Mock private ContentSkillRepository contentSkillRepository;

    @InjectMocks private CharacterFeatureGrantService service;

    private final UUID classId = UUID.randomUUID();
    private final UUID featureId = UUID.randomUUID();
    private final UUID skillId = UUID.randomUUID();

    @Test
    void noOpWhenRuntimeFlagIsOff() {
        when(flags.isRuntimeEnabled()).thenReturn(false);

        service.applyForClassLevel(PlayerCharacter.builder().id(UUID.randomUUID()).build(), classId, 1);

        verifyNoInteractions(classFeatureRepository, ruleRepository, proficiencyGrantRepository,
                skillProficiencyRepository, contentSkillRepository);
    }

    @Test
    void appliesSkillGrantWithFeatureSourceWhenEnabled() {
        when(flags.isRuntimeEnabled()).thenReturn(true);

        ClassFeature feature = ClassFeature.builder().id(featureId).level(1).build();
        when(classFeatureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(feature));

        FeatureRule rule = FeatureRule.builder()
                .id(UUID.randomUUID()).ownerType("CLASS_FEATURE").ownerId(featureId)
                .ruleType("static_grant").enabled(true).reviewStatus("approved")
                .approvedRevisionId(UUID.randomUUID()).build();
        when(ruleRepository.findByOwnerTypeAndOwnerIdIn(eq("CLASS_FEATURE"), anyList()))
                .thenReturn(List.of(rule));

        FeatureProficiencyGrant grant = FeatureProficiencyGrant.builder()
                .id(UUID.randomUUID()).featureRuleId(rule.getId())
                .proficiencyType("skill").targetId(skillId).expertise(false).grantTiming("level_up").build();
        when(proficiencyGrantRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(grant));

        when(skillProficiencyRepository.findByCharacterIdAndSkillId(any(), eq(skillId)))
                .thenReturn(Optional.empty());
        when(contentSkillRepository.findById(skillId)).thenReturn(Optional.of(new ContentSkill()));

        UUID charId = UUID.randomUUID();
        service.applyForClassLevel(PlayerCharacter.builder().id(charId).build(), classId, 1);

        ArgumentCaptor<CharacterSkillProficiency> captor = ArgumentCaptor.forClass(CharacterSkillProficiency.class);
        verify(skillProficiencyRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(SkillProficiencySource.FEATURE);
        assertThat(captor.getValue().getProficiencyLevel()).isEqualTo(SkillProficiencyLevel.PROFICIENT);
    }

    @Test
    void doesNotDuplicateExistingProficiency() {
        when(flags.isRuntimeEnabled()).thenReturn(true);

        ClassFeature feature = ClassFeature.builder().id(featureId).level(1).build();
        when(classFeatureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(feature));

        FeatureRule rule = FeatureRule.builder()
                .id(UUID.randomUUID()).ownerType("CLASS_FEATURE").ownerId(featureId)
                .ruleType("static_grant").enabled(true).reviewStatus("approved")
                .approvedRevisionId(UUID.randomUUID()).build();
        when(ruleRepository.findByOwnerTypeAndOwnerIdIn(eq("CLASS_FEATURE"), anyList()))
                .thenReturn(List.of(rule));

        FeatureProficiencyGrant grant = FeatureProficiencyGrant.builder()
                .id(UUID.randomUUID()).featureRuleId(rule.getId())
                .proficiencyType("skill").targetId(skillId).expertise(false).grantTiming("level_up").build();
        when(proficiencyGrantRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(grant));

        when(skillProficiencyRepository.findByCharacterIdAndSkillId(any(), eq(skillId)))
                .thenReturn(Optional.of(CharacterSkillProficiency.builder()
                        .proficiencyLevel(SkillProficiencyLevel.PROFICIENT).build()));

        service.applyForClassLevel(PlayerCharacter.builder().id(UUID.randomUUID()).build(), classId, 1);

        verify(skillProficiencyRepository, never()).save(any());
    }
}
