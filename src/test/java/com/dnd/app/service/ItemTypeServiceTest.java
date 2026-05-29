package com.dnd.app.service;

import com.dnd.app.dto.request.CreateItemTypeRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.mapper.ReferenceDataMapper;
import com.dnd.app.mapper.TeamMapper;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemTypeServiceTest {

    @Mock private StatTypeRepository statTypeRepository;
    @Mock private ItemTypeRepository itemTypeRepository;
    @Mock private CharacterClassRepository classRepository;
    @Mock private CharacterRaceRepository raceRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private SubclassRepository subclassRepository;
    @Mock private FeatRepository featRepository;
    @Mock private ClassLevelRewardRepository classLevelRewardRepository;
    @Mock private SkillEffectRepository skillEffectRepository;
    @Mock private BuffDebuffRepository buffDebuffRepository;
    @Mock private ReferenceDataMapper refMapper;
    @Mock private UserMapper userMapper;
    @Mock private TeamMapper teamMapper;
    @Mock private RewardResolverRegistry rewardResolverRegistry;

    @InjectMocks private AdminService adminService;

    @Test
    void create_withDamage_requiresDamageType() {
        CreateItemTypeRequest req = CreateItemTypeRequest.builder()
                .name("Sword").slot("MAIN_HAND").damageDice("2d6").damageType(null).build();
        assertThrows(BadRequestException.class, () -> adminService.createItemType(req));
    }

    @Test
    void create_withInvalidDamageType_throws400() {
        CreateItemTypeRequest req = CreateItemTypeRequest.builder()
                .name("Sword").slot("MAIN_HAND").damageDice("2d6").damageType("INVALID").build();
        assertThrows(BadRequestException.class, () -> adminService.createItemType(req));
    }

    @Test
    void create_withSkillId_requiresSkillActivation() {
        CreateItemTypeRequest req = CreateItemTypeRequest.builder()
                .name("Sword").slot("MAIN_HAND").skillId(UUID.randomUUID()).skillActivation(null).build();
        assertThrows(BadRequestException.class, () -> adminService.createItemType(req));
    }

    @Test
    void create_withActivationButNoSkill_throws400() {
        CreateItemTypeRequest req = CreateItemTypeRequest.builder()
                .name("Sword").slot("MAIN_HAND").skillId(null).skillActivation("PASSIVE").build();
        assertThrows(BadRequestException.class, () -> adminService.createItemType(req));
    }
}
