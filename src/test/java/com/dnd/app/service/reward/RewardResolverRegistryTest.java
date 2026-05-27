package com.dnd.app.service.reward;

import com.dnd.app.domain.Feat;
import com.dnd.app.domain.Skill;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.SkillRepository;
import com.dnd.app.repository.SubclassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardResolverRegistryTest {

    @Mock private SkillRepository skillRepository;
    @Mock private SubclassRepository subclassRepository;
    @Mock private FeatRepository featRepository;

    private RewardResolverRegistry registry;

    @BeforeEach
    void setUp() {
        SkillRewardResolver skillResolver = new SkillRewardResolver(skillRepository);
        SubclassRewardResolver subclassResolver = new SubclassRewardResolver(subclassRepository);
        FeatRewardResolver featResolver = new FeatRewardResolver(featRepository);
        registry = new RewardResolverRegistry(List.of(skillResolver, subclassResolver, featResolver));
    }

    @Test
    void resolve_unknownType_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("SPELL", id));
    }

    @Test
    void resolve_skill_returnsSkillDetail() {
        UUID skillId = UUID.randomUUID();
        Skill skill = Skill.builder().id(skillId).name("Extra Attack").description("Attack twice").build();
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));

        RewardDetailDto result = registry.resolve("SKILL", skillId);

        assertEquals("Extra Attack", result.getName());
        assertEquals("Attack twice", result.getDescription());
        assertEquals(skillId, result.getRewardId());
    }

    @Test
    void resolve_feat_returnsFeatDetail() {
        UUID featId = UUID.randomUUID();
        Feat feat = Feat.builder().id(featId).name("Lucky").description("3 luck points").build();
        when(featRepository.findById(featId)).thenReturn(Optional.of(feat));

        RewardDetailDto result = registry.resolve("FEAT", featId);

        assertEquals("Lucky", result.getName());
        assertEquals("3 luck points", result.getDescription());
    }

    @Test
    void validate_unknownType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> registry.validate("MANEUVER", UUID.randomUUID()));
    }
}
