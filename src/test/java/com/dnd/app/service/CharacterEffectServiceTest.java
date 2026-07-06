package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.AbilityCheckResponse;
import com.dnd.app.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterEffectService: расчёт проверок характеристик и срок действия эффектов")
class CharacterEffectServiceTest {

    @Mock private CharacterActiveEffectRepository characterActiveEffectRepository;
    @Mock private BuffDebuffRepository buffDebuffRepository;
    @Mock private PlayerCharacterRepository playerCharacterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private CampaignMemberRepository campaignMemberRepository;
    @Mock private ModifierAggregator modifierAggregator;

    @InjectMocks private CharacterEffectService characterEffectService;

    // ======================== calculateAbilityCheckModifier ========================

    @Test
    @DisplayName("Проверка характеристики только по базовому модификатору")
    void calculateAbilityCheck_baseOnly() {
        UUID characterId = UUID.randomUUID();
        UUID statTypeId = UUID.randomUUID();
        String username = "admin";

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .role(Role.ADMIN)
                .build();

        StatType strStatType = StatType.builder()
                .id(statTypeId)
                .nameRu("Strength")
                .build();

        CharacterStat strStat = CharacterStat.builder()
                .id(UUID.randomUUID())
                .statType(strStatType)
                .value(16)
                .build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId)
                .name("TestHero")
                .owner(user)
                .stats(new ArrayList<>(List.of(strStat)))
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(characterActiveEffectRepository.findByCharacterId(characterId)).thenReturn(Collections.emptyList());

        AbilityCheckResponse response = characterEffectService.calculateAbilityCheckModifier(characterId, statTypeId, username);

        assertEquals("Strength", response.getStatName());
        assertEquals(16, response.getBaseValue());
        assertEquals(3, response.getModifier());
        assertEquals(0, response.getBuffBonus());
        assertEquals(0, response.getEquipmentBonus());
        assertEquals(3, response.getTotalModifier());
    }

    @Test
    @DisplayName("Проверка характеристики с учётом баффов и дебаффов")
    void calculateAbilityCheck_withBuffsAndDebuffs() {
        UUID characterId = UUID.randomUUID();
        UUID statTypeId = UUID.randomUUID();
        String username = "admin";

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .role(Role.ADMIN)
                .build();

        StatType strStatType = StatType.builder()
                .id(statTypeId)
                .nameRu("Strength")
                .build();

        CharacterStat strStat = CharacterStat.builder()
                .id(UUID.randomUUID())
                .statType(strStatType)
                .value(14)
                .build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId)
                .name("TestHero")
                .owner(user)
                .stats(new ArrayList<>(List.of(strStat)))
                .build();

        BuffDebuff buff = BuffDebuff.builder()
                .id(UUID.randomUUID())
                .name("Bull's Strength")
                .effectType("STAT_MODIFIER")
                .isBuff(true)
                .modifierValue(2)
                .targetStat(strStatType)
                .build();

        BuffDebuff debuff = BuffDebuff.builder()
                .id(UUID.randomUUID())
                .name("Weakness")
                .effectType("STAT_MODIFIER")
                .isBuff(false)
                .modifierValue(1)
                .targetStat(strStatType)
                .build();

        CharacterActiveEffect buffEffect = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .character(character)
                .buffDebuff(buff)
                .appliedBy(user)
                .remainingRounds(5)
                .appliedAt(Instant.now())
                .build();

        CharacterActiveEffect debuffEffect = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .character(character)
                .buffDebuff(debuff)
                .appliedBy(user)
                .remainingRounds(3)
                .appliedAt(Instant.now())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(buffEffect, debuffEffect));

        AbilityCheckResponse response = characterEffectService.calculateAbilityCheckModifier(characterId, statTypeId, username);

        assertEquals(14, response.getBaseValue());
        assertEquals(2, response.getModifier());       // floor((14-10)/2.0) = 2
        assertEquals(1, response.getBuffBonus());       // buffTotal(2) - debuffTotal(1) = 1
        assertEquals(0, response.getEquipmentBonus());
        assertEquals(3, response.getTotalModifier());   // 2 + 2 - 1 = 3
    }

    @Test
    void calculateAbilityCheck_noStatFound_defaultsTo10() {
        UUID characterId = UUID.randomUUID();
        UUID statTypeId = UUID.randomUUID();
        String username = "admin";

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .role(Role.ADMIN)
                .build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId)
                .name("TestHero")
                .owner(user)
                .stats(new ArrayList<>())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(characterActiveEffectRepository.findByCharacterId(characterId)).thenReturn(Collections.emptyList());

        AbilityCheckResponse response = characterEffectService.calculateAbilityCheckModifier(characterId, statTypeId, username);

        assertEquals(10, response.getBaseValue());
        assertEquals(0, response.getModifier());        // floor((10-10)/2.0) = 0
        assertEquals(0, response.getBuffBonus());
        assertEquals(0, response.getTotalModifier());
        assertEquals("Unknown", response.getStatName());
    }

    @Test
    @DisplayName("Несоответствующие эффекты игнорируются при проверке")
    void calculateAbilityCheck_nonMatchingEffectsIgnored() {
        UUID characterId = UUID.randomUUID();
        UUID statTypeId = UUID.randomUUID();
        UUID otherStatTypeId = UUID.randomUUID();
        String username = "admin";

        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .role(Role.ADMIN)
                .build();

        StatType strStatType = StatType.builder()
                .id(statTypeId)
                .nameRu("Strength")
                .build();

        StatType dexStatType = StatType.builder()
                .id(otherStatTypeId)
                .nameRu("Dexterity")
                .build();

        CharacterStat strStat = CharacterStat.builder()
                .id(UUID.randomUUID())
                .statType(strStatType)
                .value(12)
                .build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId)
                .name("TestHero")
                .owner(user)
                .stats(new ArrayList<>(List.of(strStat)))
                .build();

        // Effect targets a different stat (Dexterity instead of Strength)
        BuffDebuff differentStatBuff = BuffDebuff.builder()
                .id(UUID.randomUUID())
                .name("Cat's Grace")
                .effectType("STAT_MODIFIER")
                .isBuff(true)
                .modifierValue(4)
                .targetStat(dexStatType)
                .build();

        // Effect has wrong effectType (DAMAGE instead of STAT_MODIFIER)
        BuffDebuff wrongTypeBuff = BuffDebuff.builder()
                .id(UUID.randomUUID())
                .name("Fire Shield")
                .effectType("DAMAGE")
                .isBuff(true)
                .modifierValue(3)
                .targetStat(strStatType)
                .build();

        CharacterActiveEffect effectDifferentStat = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .character(character)
                .buffDebuff(differentStatBuff)
                .appliedBy(user)
                .remainingRounds(5)
                .appliedAt(Instant.now())
                .build();

        CharacterActiveEffect effectWrongType = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .character(character)
                .buffDebuff(wrongTypeBuff)
                .appliedBy(user)
                .remainingRounds(5)
                .appliedAt(Instant.now())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(effectDifferentStat, effectWrongType));

        AbilityCheckResponse response = characterEffectService.calculateAbilityCheckModifier(characterId, statTypeId, username);

        assertEquals(12, response.getBaseValue());
        assertEquals(1, response.getModifier());        // floor((12-10)/2.0) = 1
        assertEquals(0, response.getBuffBonus());       // no matching effects
        assertEquals(0, response.getEquipmentBonus());
        assertEquals(1, response.getTotalModifier());   // only base modifier
    }

    // ======================== decrementRounds ========================

    @Test
    @DisplayName("Уменьшение раундов до нуля снимает эффект")
    void decrementRounds_expiresEffect() {
        UUID characterId = UUID.randomUUID();

        CharacterActiveEffect expiringEffect = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .remainingRounds(1)
                .appliedAt(Instant.now())
                .build();

        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(expiringEffect));

        characterEffectService.decrementRounds(characterId);

        verify(characterActiveEffectRepository).delete(expiringEffect);
        verify(characterActiveEffectRepository, never()).save(any());
    }

    @Test
    @DisplayName("Уменьшение раундов снижает счётчик у действующего эффекта")
    void decrementRounds_decrementsNonExpired() {
        UUID characterId = UUID.randomUUID();

        CharacterActiveEffect activeEffect = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .remainingRounds(3)
                .appliedAt(Instant.now())
                .build();

        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(activeEffect));

        characterEffectService.decrementRounds(characterId);

        verify(characterActiveEffectRepository, never()).delete(any());
        verify(characterActiveEffectRepository).save(activeEffect);
        assertEquals(2, activeEffect.getRemainingRounds());
    }

    @Test
    @DisplayName("Постоянный эффект не изменяется при уменьшении раундов")
    void decrementRounds_permanentEffectUnchanged() {
        UUID characterId = UUID.randomUUID();

        CharacterActiveEffect permanentEffect = CharacterActiveEffect.builder()
                .id(UUID.randomUUID())
                .remainingRounds(null)
                .appliedAt(Instant.now())
                .build();

        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(permanentEffect));

        characterEffectService.decrementRounds(characterId);

        verify(characterActiveEffectRepository, never()).delete(any());
        verify(characterActiveEffectRepository, never()).save(any());
        assertNull(permanentEffect.getRemainingRounds());
    }
}
