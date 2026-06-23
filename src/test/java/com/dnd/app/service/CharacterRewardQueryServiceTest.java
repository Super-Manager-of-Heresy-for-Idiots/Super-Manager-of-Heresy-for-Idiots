package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterRewardsResponse;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterRewardQueryService: derive-on-read AUTO-грантов")
class CharacterRewardQueryServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private CharacterRewardSelectionRepository selectionRepository;
    @Mock private ContentCharacterClassRepository contentClassRepository;
    @Mock private ClassLevelRewardGroupRepository rewardGroupRepository;

    @InjectMocks private CharacterRewardQueryService service;

    @Test
    @DisplayName("AUTO FEATURE-гранты до уровня персонажа выводятся, выше уровня — нет")
    void derivesAutoFeaturesUpToCharacterLevel() {
        UUID characterId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        User user = User.builder().id(userId).username("player").role(Role.PLAYER).build();
        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId).owner(user).totalLevel(2).build();

        when(userRepository.findByUsername("player")).thenReturn(Optional.of(user));
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(classLevelRepository.findAllByCharacterId(characterId))
                .thenReturn(List.of(CharacterClassLevel.builder()
                        .characterId(characterId).classId(classId).classLevel(2).build()));
        when(selectionRepository.findAllByCharacterId(characterId)).thenReturn(List.of());
        when(contentClassRepository.findById(classId)).thenReturn(Optional.of(
                ContentCharacterClass.builder().id(classId).nameRu("Воин").build()));

        ClassLevelRewardGrant lvl1Feature = ClassLevelRewardGrant.builder()
                .id(UUID.randomUUID()).grantType("FEATURE").labelRu("Второе дыхание").build();
        ClassLevelRewardGroup lvl1Auto = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(1).groupKind("AUTO")
                .grants(List.of(lvl1Feature)).build();

        ClassLevelRewardGrant lvl3Feature = ClassLevelRewardGrant.builder()
                .id(UUID.randomUUID()).grantType("FEATURE").labelRu("Слишком высокий уровень").build();
        ClassLevelRewardGroup lvl3Auto = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("AUTO")
                .grants(List.of(lvl3Feature)).build();

        when(rewardGroupRepository.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(lvl1Auto, lvl3Auto));

        CharacterRewardsResponse response = service.getCharacterRewards(characterId, "player");

        assertEquals(1, response.getClassBreakdown().size());
        Map<String, List<CharacterRewardsResponse.AcquiredReward>> byType =
                response.getClassBreakdown().get(0).getRewardsByType();
        assertNotNull(byType.get("FEATURE"));
        assertEquals(1, byType.get("FEATURE").size());
        assertEquals("Второе дыхание", byType.get("FEATURE").get(0).getName());
    }
}
