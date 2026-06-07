package com.dnd.app.service;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.Skill;
import com.dnd.app.domain.SkillEffect;
import com.dnd.app.dto.request.SetSkillEffectsRequest;
import com.dnd.app.dto.request.SkillEffectRequest;
import com.dnd.app.dto.response.SkillEffectResponse;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.mapper.ReferenceDataMapper;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private StatTypeRepository statTypeRepository;
    @Mock private ItemTypeRepository itemTypeRepository;
    @Mock private CharacterClassRepository classRepository;
    @Mock private CharacterRaceRepository raceRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private SubclassRepository subclassRepository;
    @Mock private FeatRepository featRepository;
    @Mock private ClassLevelRewardRepository classLevelRewardRepository;
    @Mock private SkillEffectRepository skillEffectRepository;
    @Mock private BuffDebuffRepository buffDebuffRepository;
    @Mock private ReferenceDataMapper refMapper;
    @Mock private UserMapper userMapper;
    @Mock private RewardResolverRegistry rewardResolverRegistry;

    @InjectMocks private AdminService adminService;

    @Test
    void setEffects_replacesAllExistingEffects() {
        UUID skillId = UUID.randomUUID();
        Skill skill = Skill.builder().id(skillId).name("Fire Ball").damageBonus(0).build();
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        UUID bdId = UUID.randomUUID();
        BuffDebuff bd = BuffDebuff.builder().id(bdId).name("Burning").isBuff(false).build();
        when(buffDebuffRepository.findById(bdId)).thenReturn(Optional.of(bd));

        SkillEffectRequest ser = SkillEffectRequest.builder()
                .buffDebuffId(bdId).effectRole("DEBUFF").chancePercent(75).build();
        SetSkillEffectsRequest req = SetSkillEffectsRequest.builder().effects(List.of(ser)).build();

        when(skillEffectRepository.save(any())).thenAnswer(inv -> {
            SkillEffect se = inv.getArgument(0);
            se.setId(UUID.randomUUID());
            return se;
        });

        List<SkillEffectResponse> result = adminService.setSkillEffects(skillId, req);

        verify(skillEffectRepository).deleteAllBySkillId(skillId);
        assertEquals(1, result.size());
        assertEquals("DEBUFF", result.get(0).getEffectRole());
    }

    @Test
    void setEffects_buffRoleOnDebuffRow_throws422() {
        UUID skillId = UUID.randomUUID();
        Skill skill = Skill.builder().id(skillId).name("Curse").damageBonus(0).build();
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        UUID bdId = UUID.randomUUID();
        BuffDebuff bd = BuffDebuff.builder().id(bdId).name("Poisoned").isBuff(false).build();
        when(buffDebuffRepository.findById(bdId)).thenReturn(Optional.of(bd));

        SkillEffectRequest ser = SkillEffectRequest.builder()
                .buffDebuffId(bdId).effectRole("BUFF").chancePercent(100).build();
        SetSkillEffectsRequest req = SetSkillEffectsRequest.builder().effects(List.of(ser)).build();

        assertThrows(UnprocessableEntityException.class, () -> adminService.setSkillEffects(skillId, req));
    }

    @Test
    void setEffects_emptyList_removesAllEffects() {
        UUID skillId = UUID.randomUUID();
        Skill skill = Skill.builder().id(skillId).name("Slash").damageBonus(0).build();
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        SetSkillEffectsRequest req = SetSkillEffectsRequest.builder().effects(List.of()).build();
        List<SkillEffectResponse> result = adminService.setSkillEffects(skillId, req);

        verify(skillEffectRepository).deleteAllBySkillId(skillId);
        assertTrue(result.isEmpty());
    }
}
