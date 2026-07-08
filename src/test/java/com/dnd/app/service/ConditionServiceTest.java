package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.BattleCombatantCondition;
import com.dnd.app.domain.BestiaryCondition;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.CombatantConditionResponse;
import com.dnd.app.repository.BattleCombatantConditionRepository;
import com.dnd.app.repository.BestiaryConditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConditionService: apply(upsert)/remove/tick (1.1)")
class ConditionServiceTest {

    @Mock private BattleCombatantConditionRepository conditionRepository;
    @Mock private BestiaryConditionRepository bestiaryConditionRepository;
    @Mock private WebSocketEventService webSocketEventService;

    private ConditionService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();
    private final UUID combatantId = UUID.randomUUID();
    private final UUID conditionId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private BattleCombatant combatant;
    private BestiaryCondition prone;

    @BeforeEach
    void setUp() {
        service = new ConditionService(conditionRepository, bestiaryConditionRepository, webSocketEventService);
        combatant = BattleCombatant.builder().id(combatantId).build();
        prone = BestiaryCondition.builder().id(conditionId).code("prone").nameRusloc("Лежит").build();
    }

    private BattleCombatantCondition instance(Integer remaining) {
        return BattleCombatantCondition.builder()
                .id(UUID.randomUUID()).combatant(combatant).condition(prone)
                .sourceText("Заклинание").remainingRounds(remaining).build();
    }

    @Test
    @DisplayName("apply нового: сохраняет и публикует COMBATANT_CONDITIONS_CHANGED")
    void applyNew_savesAndPublishes() {
        when(bestiaryConditionRepository.findById(conditionId)).thenReturn(Optional.of(prone));
        when(conditionRepository.findByCombatantIdAndConditionId(combatantId, conditionId)).thenReturn(Optional.empty());
        when(conditionRepository.findByCombatantId(combatantId)).thenReturn(List.of(instance(3)));

        List<CombatantConditionResponse> result =
                service.apply(campaignId, combatant, conditionId, "Заклинание", 3, actorId, 1);

        verify(conditionRepository).save(any(BattleCombatantCondition.class));
        verify(webSocketEventService).sendCampaignEvent(
                eq(WebSocketEventType.COMBATANT_CONDITIONS_CHANGED), eq(campaignId), any(), eq(actorId));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Лежит");
    }

    @Test
    @DisplayName("apply дубликата: обновляет существующую запись (upsert), а не плодит вторую")
    void applyDuplicate_updatesExisting() {
        BattleCombatantCondition existing = instance(1);
        when(bestiaryConditionRepository.findById(conditionId)).thenReturn(Optional.of(prone));
        when(conditionRepository.findByCombatantIdAndConditionId(combatantId, conditionId)).thenReturn(Optional.of(existing));
        when(conditionRepository.findByCombatantId(combatantId)).thenReturn(List.of(existing));

        service.apply(campaignId, combatant, conditionId, "Новый источник", 5, actorId, 2);

        ArgumentCaptor<BattleCombatantCondition> captor = ArgumentCaptor.forClass(BattleCombatantCondition.class);
        verify(conditionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId()); // same row
        assertThat(captor.getValue().getRemainingRounds()).isEqualTo(5);
        assertThat(captor.getValue().getSourceText()).isEqualTo("Новый источник");
    }

    @Test
    @DisplayName("tick: конечная длительность декрементится, 1→снимается, null не трогается")
    void tick_decrementsAndExpires() {
        BattleCombatantCondition two = instance(2);
        BattleCombatantCondition one = instance(1);
        BattleCombatantCondition indefinite = instance(null);
        when(conditionRepository.findByCombatant_Battle_Id(battleId)).thenReturn(List.of(two, one, indefinite));

        service.tick(battleId);

        assertThat(two.getRemainingRounds()).isEqualTo(1);
        verify(conditionRepository).delete(one);
        verify(conditionRepository, never()).delete(two);
        verify(conditionRepository, never()).delete(indefinite);
    }
}
