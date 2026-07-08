package com.dnd.app.service;

import com.dnd.app.domain.BattleLog;
import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.BattleLogEntryResponse;
import com.dnd.app.repository.BattleLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single write point for the persistent combat log (Phase 1.2). Every meaningful battle event is
 * appended here from the flow that performs it, in the SAME transaction, so the log can never
 * diverge from state. {@code seq} is assigned as {@code max(seq)+1} per battle and the row is flushed
 * immediately so a second append within one action (ATTACK then DAMAGE) gets the next number.
 *
 * PUBLIC entries are also pushed live over the campaign topic; GM_ONLY entries are pulled by the GM
 * via the log API only (there is no GM-scoped WS topic in the MVP — a documented limitation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleLogService {

    private static final int MAX_PAGE = 200;

    private final BattleLogRepository logRepository;
    private final WebSocketEventService webSocketEventService;
    private final ObjectMapper objectMapper;

    @Transactional
    public BattleLog append(UUID battleId, UUID campaignId, BattleLogType type,
                            UUID actorCombatantId, UUID targetCombatantId,
                            Map<String, Object> payload, BattleLogVisibility visibility, UUID actorUserId) {
        Long max = logRepository.findMaxSeq(battleId);
        long seq = (max == null ? 0L : max) + 1;
        BattleLog entry = BattleLog.builder()
                .battleId(battleId)
                .seq(seq)
                .type(type)
                .actorCombatantId(actorCombatantId)
                .targetCombatantId(targetCombatantId)
                .payload(writeJson(payload))
                .visibility(visibility == null ? BattleLogVisibility.PUBLIC : visibility)
                .build();
        entry = logRepository.saveAndFlush(entry);

        if (entry.getVisibility() == BattleLogVisibility.PUBLIC && campaignId != null) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_LOG_APPENDED, campaignId,
                    toDto(entry), actorUserId);
        }
        return entry;
    }

    @Transactional(readOnly = true)
    public List<BattleLogEntryResponse> list(UUID battleId, long afterSeq, int limit, boolean includeGmOnly) {
        int capped = limit <= 0 ? MAX_PAGE : Math.min(limit, MAX_PAGE);
        Pageable page = PageRequest.of(0, capped);
        List<BattleLog> rows = includeGmOnly
                ? logRepository.findByBattleIdAndSeqGreaterThanOrderBySeqAsc(battleId, afterSeq, page)
                : logRepository.findByBattleIdAndSeqGreaterThanAndVisibilityNotOrderBySeqAsc(
                        battleId, afterSeq, BattleLogVisibility.GM_ONLY, page);
        return rows.stream().map(this::toDto).toList();
    }

    private BattleLogEntryResponse toDto(BattleLog e) {
        return BattleLogEntryResponse.builder()
                .id(e.getId())
                .seq(e.getSeq())
                .type(e.getType().name())
                .actorCombatantId(e.getActorCombatantId())
                .targetCombatantId(e.getTargetCombatantId())
                .payload(readJson(e.getPayload()))
                .visibility(e.getVisibility().name())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private String writeJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize battle-log payload: {}", ex.getMessage());
            return null;
        }
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            return json; // keep the raw string rather than dropping data
        }
    }
}
