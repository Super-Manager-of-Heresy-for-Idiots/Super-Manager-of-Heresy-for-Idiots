package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.repository.BattleCombatantRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterHpServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private BattleCombatantRepository combatantRepository;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private GameplayEventService gameplayEventService;

    @InjectMocks private CharacterHpService service;

    private final UUID characterId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void damageDrainsTempHpFirstThenCurrentHp() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).currentHp(10).maxHp(10).tempHp(5).build();
        when(characterRepository.findByIdForUpdate(characterId)).thenReturn(Optional.of(c));
        when(combatantRepository.findByCharacter_IdAndBattle_Status(characterId, BattleStatus.ACTIVE))
                .thenReturn(List.of());

        HpChangeResult result = service.applyDelta(characterId, -8, campaignId, actorUserId);

        // 8 damage: 5 absorbed by temp HP, remaining 3 off current HP.
        assertThat(c.getTempHp()).isZero();
        assertThat(c.getCurrentHp()).isEqualTo(7);
        assertThat(result.currentHp()).isEqualTo(7);
        assertThat(result.tempHp()).isZero();
        assertThat(result.reachedZero()).isFalse();
        verify(characterRepository).save(c);
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.HP_CHANGED), eq(campaignId),
                eq(characterId), any(), eq(actorUserId));
        verify(gameplayEventService, never()).publish(any(), anyString(), any(), anyString());
    }

    @Test
    void mirrorsHpOntoActiveCombatants() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).currentHp(20).maxHp(20).tempHp(0).build();
        BattleCombatant combatant = BattleCombatant.builder()
                .id(UUID.randomUUID()).currentHp(20).maxHp(20).build();
        when(characterRepository.findByIdForUpdate(characterId)).thenReturn(Optional.of(c));
        when(combatantRepository.findByCharacter_IdAndBattle_Status(characterId, BattleStatus.ACTIVE))
                .thenReturn(List.of(combatant));

        service.applyDelta(characterId, -7, campaignId, actorUserId);

        assertThat(combatant.getCurrentHp()).isEqualTo(13);
        verify(combatantRepository).save(combatant);
    }

    @Test
    void reachingZeroPublishesGameplayHook() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).currentHp(4).maxHp(20).tempHp(0).build();
        when(characterRepository.findByIdForUpdate(characterId)).thenReturn(Optional.of(c));
        when(combatantRepository.findByCharacter_IdAndBattle_Status(characterId, BattleStatus.ACTIVE))
                .thenReturn(List.of());

        HpChangeResult result = service.applyDelta(characterId, -10, campaignId, actorUserId);

        assertThat(result.currentHp()).isZero();
        assertThat(result.reachedZero()).isTrue();
        verify(gameplayEventService).publish(eq(c), eq("hp_reached_zero"), any(), anyString());
    }

    @Test
    void tempHpDoesNotStack() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).currentHp(10).maxHp(10).tempHp(5).build();
        when(characterRepository.findByIdForUpdate(characterId)).thenReturn(Optional.of(c));

        // A smaller grant is ignored; a larger one replaces the pool (temp HP does not stack).
        service.applyTempHp(characterId, 3, campaignId, actorUserId);
        assertThat(c.getTempHp()).isEqualTo(5);

        service.applyTempHp(characterId, 8, campaignId, actorUserId);
        assertThat(c.getTempHp()).isEqualTo(8);
    }
}
