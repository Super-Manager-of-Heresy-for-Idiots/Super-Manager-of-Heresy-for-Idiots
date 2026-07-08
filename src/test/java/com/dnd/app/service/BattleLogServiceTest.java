package com.dnd.app.service;

import com.dnd.app.domain.BattleLog;
import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.BattleLogEntryResponse;
import com.dnd.app.repository.BattleLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BattleLogServiceTest {

    private BattleLogRepository logRepository;
    private WebSocketEventService webSocketEventService;
    private BattleLogService service;

    private final UUID battleId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        logRepository = mock(BattleLogRepository.class);
        webSocketEventService = mock(WebSocketEventService.class);
        service = new BattleLogService(logRepository, webSocketEventService, new ObjectMapper());
        when(logRepository.saveAndFlush(any(BattleLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void append_assignsNextSeq_serializesPayload_andBroadcastsPublicEntry() {
        when(logRepository.findMaxSeq(battleId)).thenReturn(4L);

        service.append(battleId, campaignId, BattleLogType.ATTACK, null, null,
                Map.of("outcome", "HIT", "damage", 7), BattleLogVisibility.PUBLIC, actorUserId);

        ArgumentCaptor<BattleLog> saved = ArgumentCaptor.forClass(BattleLog.class);
        verify(logRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSeq()).isEqualTo(5L);
        assertThat(saved.getValue().getType()).isEqualTo(BattleLogType.ATTACK);
        assertThat(saved.getValue().getPayload()).contains("\"outcome\":\"HIT\"").contains("\"damage\":7");
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.BATTLE_LOG_APPENDED),
                eq(campaignId), any(), eq(actorUserId));
    }

    @Test
    void append_seedsSeqToOne_whenLogEmpty() {
        when(logRepository.findMaxSeq(battleId)).thenReturn(null);

        service.append(battleId, campaignId, BattleLogType.TURN, null, null,
                Map.of("round", 1), BattleLogVisibility.PUBLIC, actorUserId);

        ArgumentCaptor<BattleLog> saved = ArgumentCaptor.forClass(BattleLog.class);
        verify(logRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getSeq()).isEqualTo(1L);
    }

    @Test
    void append_doesNotBroadcast_forGmOnlyEntry() {
        when(logRepository.findMaxSeq(battleId)).thenReturn(0L);

        service.append(battleId, campaignId, BattleLogType.DEATH_SAVE, null, null,
                Map.of("event", "AUTO_FAIL", "failures", 2), BattleLogVisibility.GM_ONLY, actorUserId);

        verify(logRepository).saveAndFlush(any(BattleLog.class));
        verifyNoInteractions(webSocketEventService);
    }

    @Test
    void list_asPlayer_excludesGmOnly_andCapsLimit() {
        when(logRepository.findByBattleIdAndSeqGreaterThanAndVisibilityNotOrderBySeqAsc(
                eq(battleId), eq(3L), eq(BattleLogVisibility.GM_ONLY), any(Pageable.class)))
                .thenReturn(List.of());

        service.list(battleId, 3L, 999, false);

        verify(logRepository).findByBattleIdAndSeqGreaterThanAndVisibilityNotOrderBySeqAsc(
                eq(battleId), eq(3L), eq(BattleLogVisibility.GM_ONLY), any(Pageable.class));
        verify(logRepository, never())
                .findByBattleIdAndSeqGreaterThanOrderBySeqAsc(any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void list_asGm_includesEverything_andParsesPayload() {
        BattleLog row = BattleLog.builder()
                .id(UUID.randomUUID())
                .battleId(battleId)
                .seq(1L)
                .type(BattleLogType.ATTACK)
                .payload("{\"outcome\":\"HIT\",\"damage\":7}")
                .visibility(BattleLogVisibility.PUBLIC)
                .build();
        when(logRepository.findByBattleIdAndSeqGreaterThanOrderBySeqAsc(eq(battleId), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(row));

        List<BattleLogEntryResponse> out = service.list(battleId, 0L, 0, true);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getType()).isEqualTo("ATTACK");
        assertThat(out.get(0).getPayload()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) out.get(0).getPayload();
        assertThat(payload).containsEntry("outcome", "HIT").containsEntry("damage", 7);
    }
}
