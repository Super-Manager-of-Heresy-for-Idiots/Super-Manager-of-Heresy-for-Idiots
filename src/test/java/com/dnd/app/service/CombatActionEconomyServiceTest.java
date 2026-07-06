package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.BattleCombatantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatActionEconomyServiceTest {

    @Mock private BattleCombatantRepository combatantRepository;
    @Mock private WebSocketEventService webSocketEventService;

    @InjectMocks private CombatActionEconomyService service;

    private final UUID combatId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    private BattleCombatant combatant(int actionSpent, int actionMax, boolean reactionUsed) {
        Campaign campaign = Campaign.builder().id(campaignId).build();
        Battle battle = Battle.builder().id(combatId).campaign(campaign).build();
        return BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle)
                .actionSpent(actionSpent).actionMax(actionMax)
                .bonusActionSpent(0).bonusActionMax(1)
                .reactionUsed(reactionUsed).build();
    }

    @Test
    void spendActionMarksActionSpentAndBroadcasts() {
        BattleCombatant c = combatant(0, 1, false);
        when(combatantRepository.findByBattleIdAndCharacterIdForUpdate(combatId, characterId))
                .thenReturn(Optional.of(c));

        service.spend(combatId, characterId, "action");

        assertThat(c.getActionSpent()).isEqualTo(1);
        verify(combatantRepository).save(c);
        verify(webSocketEventService).sendCampaignEvent(
                eq(WebSocketEventType.BATTLE_ACTION), eq(campaignId), any(), isNull());
    }

    @Test
    void spendSecondActionThrowsWithoutSaving() {
        BattleCombatant c = combatant(1, 1, false);
        when(combatantRepository.findByBattleIdAndCharacterIdForUpdate(combatId, characterId))
                .thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.spend(combatId, characterId, "action"))
                .isInstanceOf(BadRequestException.class);
        verify(combatantRepository, never()).save(any());
    }

    @Test
    void spendSecondReactionThrows() {
        BattleCombatant c = combatant(0, 1, true);
        when(combatantRepository.findByBattleIdAndCharacterIdForUpdate(combatId, characterId))
                .thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.spend(combatId, characterId, "reaction"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void costFreeCodeIsNoOp() {
        service.spend(combatId, characterId, "free_action");

        verify(combatantRepository, never()).findByBattleIdAndCharacterIdForUpdate(any(), any());
        verify(combatantRepository, never()).save(any());
    }

    @Test
    void spendThrowsWhenCharacterNotInCombat() {
        when(combatantRepository.findByBattleIdAndCharacterIdForUpdate(combatId, characterId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.spend(combatId, characterId, "action"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void canSpendReflectsSlotState() {
        when(combatantRepository.findByBattleIdAndCharacterId(combatId, characterId))
                .thenReturn(Optional.of(combatant(0, 1, false)));
        assertThat(service.canSpend(combatId, characterId, "action")).isTrue();

        when(combatantRepository.findByBattleIdAndCharacterId(combatId, characterId))
                .thenReturn(Optional.of(combatant(1, 1, false)));
        assertThat(service.canSpend(combatId, characterId, "action")).isFalse();

        // cost-free codes are always spendable
        assertThat(service.canSpend(combatId, characterId, "free_action")).isTrue();
    }
}
