package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.featurerule.CapabilityProfileResponse;
import com.dnd.app.repository.CharacterFeatureChoiceRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureAllowedMonsterFilterRepository;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterCapabilityProfileServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private CharacterFeatureResolver featureResolver;
    @Mock private FeatureRulesProperties featureRules;
    @Mock private FeatureResourceService featureResourceService;
    @Mock private FeatureActionService featureActionService;
    @Mock private FeatureCompanionService featureCompanionService;
    @Mock private FeatureSpellGrantService featureSpellGrantService;
    @Mock private CharacterFormService characterFormService;
    @Mock private PendingGameplayPromptService pendingGameplayPromptService;
    @Mock private FeatureActiveEffectRepository effectRepository;
    @Mock private FeatureAllowedMonsterFilterRepository allowedMonsterFilterRepository;
    @Mock private FeatureChoiceGroupRepository choiceGroupRepository;
    @Mock private CharacterFeatureChoiceRepository choiceRepository;

    @InjectMocks private CharacterCapabilityProfileService service;

    private final UUID charId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID abilityId = UUID.randomUUID();

    private PlayerCharacter character(int totalLevel, int classLevel, List<CharacterStat> stats) {
        PlayerCharacter c = PlayerCharacter.builder().id(charId).totalLevel(totalLevel).build();
        c.setClassLevels(List.of(CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(classLevel).build()));
        c.setStats(stats);
        return c;
    }

    private StatType intelligence() {
        return StatType.builder().id(abilityId).slug("int").nameRu("Интеллект").nameEn("Intelligence").build();
    }

    @Test
    void nonCasterHasNoSpellcastingAndNoFeatureFlags() {
        when(characterRepository.findById(charId)).thenReturn(Optional.of(character(3, 3, List.of())));
        when(classRepository.findById(classId)).thenReturn(Optional.of(
                ContentCharacterClass.builder().id(classId).spellcaster(false)
                        .nameRu("Варвар").nameEn("Barbarian").build()));

        CapabilityProfileResponse p = service.build(charId);

        assertThat(p.getSpellcasting().isCaster()).isFalse();
        assertThat(p.getSpellcasting().getCasterType()).isEqualTo("NONE");
        assertThat(p.isHasFeatureResources()).isFalse();
        assertThat(p.isHasFeatureActions()).isFalse();
        assertThat(p.getWildShape()).isNull();
        assertThat(p.getClasses()).hasSize(1);
        assertThat(p.getClasses().get(0).getClassNameEn()).isEqualTo("Barbarian");
    }

    @Test
    void fullCasterComputesSaveDcAndAttack() {
        CharacterStat intel = CharacterStat.builder().statType(intelligence()).value(16).build();
        when(characterRepository.findById(charId)).thenReturn(Optional.of(character(5, 5, List.of(intel))));
        when(classRepository.findById(classId)).thenReturn(Optional.of(
                ContentCharacterClass.builder().id(classId).spellcaster(true).halfCaster(false)
                        .hasCantrips(true).spellcastingAbility(intelligence())
                        .nameRu("Волшебник").nameEn("Wizard").build()));

        CapabilityProfileResponse p = service.build(charId);

        // Level 5 => PB 3; INT 16 => +3 mod. DC = 8+3+3 = 14, attack = 3+3 = 6.
        assertThat(p.getProficiencyBonus()).isEqualTo(3);
        assertThat(p.getSpellcasting().isCaster()).isTrue();
        assertThat(p.getSpellcasting().getCasterType()).isEqualTo("FULL");
        assertThat(p.getSpellcasting().isHasCantrips()).isTrue();
        assertThat(p.getSpellcasting().getAbilityId()).isEqualTo(abilityId);
        assertThat(p.getSpellcasting().getSpellSaveDc()).isEqualTo(14);
        assertThat(p.getSpellcasting().getSpellAttackBonus()).isEqualTo(6);
    }

    @Test
    void halfCasterReportsHalfType() {
        when(characterRepository.findById(charId)).thenReturn(Optional.of(character(6, 6, List.of())));
        when(classRepository.findById(classId)).thenReturn(Optional.of(
                ContentCharacterClass.builder().id(classId).spellcaster(true).halfCaster(true)
                        .nameRu("Паладин").nameEn("Paladin").build()));

        CapabilityProfileResponse p = service.build(charId);

        assertThat(p.getSpellcasting().isCaster()).isTrue();
        assertThat(p.getSpellcasting().getCasterType()).isEqualTo("HALF");
        // No matching stat => DC/attack left unknown.
        assertThat(p.getSpellcasting().getSpellSaveDc()).isNull();
    }

    @Test
    void runtimeOnPopulatesResourcePresenceFlag() {
        when(characterRepository.findById(charId)).thenReturn(Optional.of(character(2, 2, List.of())));
        when(classRepository.findById(classId)).thenReturn(Optional.of(
                ContentCharacterClass.builder().id(classId).spellcaster(false)
                        .nameRu("Варвар").nameEn("Barbarian").build()));
        when(featureRules.isRuntimeEnabled()).thenReturn(true);
        when(featureRules.resourcesActive()).thenReturn(true);
        when(featureResolver.knownBaseClassFeatures(charId)).thenReturn(List.<ClassFeature>of());
        when(featureResolver.approvedEnabledRules(anyCollection())).thenReturn(List.of());
        when(featureResourceService.list(charId)).thenReturn(List.of(
                com.dnd.app.domain.featurerule.CharacterFeatureResource.builder().id(UUID.randomUUID()).build()));

        CapabilityProfileResponse p = service.build(charId);

        assertThat(p.isRuntimeEnabled()).isTrue();
        assertThat(p.isHasFeatureResources()).isTrue();
        assertThat(p.getPendingChoices()).isZero();
    }
}
