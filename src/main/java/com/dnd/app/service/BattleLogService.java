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
 * Класс BattleLogService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleLogService {

    private static final int MAX_PAGE = 200;

    private final BattleLogRepository logRepository;
    private final WebSocketEventService webSocketEventService;
    private final ObjectMapper objectMapper;

    /**
     * Выполняет операции "append" в рамках бизнес-логики домена.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param type входящее значение type, используемое бизнес-сценарием
     * @param actorCombatantId идентификатор actor combatant, используемый для выбора нужного бизнес-объекта
     * @param targetCombatantId идентификатор target combatant, используемый для выбора нужного бизнес-объекта
     * @param payload входящее значение payload, используемое бизнес-сценарием
     * @param visibility входящее значение visibility, используемое бизнес-сценарием
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BattleLog append(UUID battleId, UUID campaignId, BattleLogType type,
                            UUID actorCombatantId, UUID targetCombatantId,
                            Map<String, Object> payload, BattleLogVisibility visibility, UUID actorUserId) {
        return append(battleId, campaignId, type, actorCombatantId, targetCombatantId,
                payload, visibility, actorUserId, null);
    }

    /**
     * Добавляет запись в журнал боя с опциональной «обратной дельтой» (фаза 3.5). Если {@code undoPayload}
     * не пуст, запись становится обратимой — её сможет откатить {@code POST /undo} (например, {@code
     * {kind:HP, combatantId, delta}}). Записи без него необратимы. Прочие параметры — как в базовом append.
     *
     * @param battleId          идентификатор боя
     * @param campaignId        идентификатор кампании (для рассылки события; может быть null)
     * @param type              тип записи журнала
     * @param actorCombatantId  комбатант-инициатор (nullable)
     * @param targetCombatantId комбатант-цель (nullable)
     * @param payload           отображаемая нагрузка записи
     * @param visibility        видимость записи (PUBLIC по умолчанию)
     * @param actorUserId       пользователь-инициатор (для WS-события)
     * @param undoPayload       «обратная дельта» для отката (nullable — тогда запись необратима)
     * @return сохранённая запись журнала
     */
    @Transactional
    public BattleLog append(UUID battleId, UUID campaignId, BattleLogType type,
                            UUID actorCombatantId, UUID targetCombatantId,
                            Map<String, Object> payload, BattleLogVisibility visibility, UUID actorUserId,
                            Map<String, Object> undoPayload) {
        Long max = logRepository.findMaxSeq(battleId);
        long seq = (max == null ? 0L : max) + 1;
        BattleLog entry = BattleLog.builder()
                .battleId(battleId)
                .seq(seq)
                .type(type)
                .actorCombatantId(actorCombatantId)
                .targetCombatantId(targetCombatantId)
                .payload(writeJson(payload))
                .undoPayload(writeJson(undoPayload))
                .visibility(visibility == null ? BattleLogVisibility.PUBLIC : visibility)
                .build();
        entry = logRepository.saveAndFlush(entry);

        if (entry.getVisibility() == BattleLogVisibility.PUBLIC && campaignId != null) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_LOG_APPENDED, campaignId,
                    toDto(entry), actorUserId);
        }
        return entry;
    }

    /**
     * Последняя ещё не откатанная обратимая операция боя (фаза 3.5) — или {@code Optional.empty()}, если
     * откатывать нечего.
     *
     * @param battleId идентификатор боя
     * @return самая свежая обратимая запись журнала
     */
    @Transactional(readOnly = true)
    public java.util.Optional<BattleLog> findLastUndoable(UUID battleId) {
        return logRepository.findFirstByBattleIdAndUndoneFalseAndUndoPayloadIsNotNullOrderBySeqDesc(battleId);
    }

    /**
     * Помечает запись журнала как откатанную (фаза 3.5), чтобы её нельзя было откатить повторно.
     *
     * @param entry запись журнала для пометки
     */
    @Transactional
    public void markUndone(BattleLog entry) {
        entry.setUndone(true);
        logRepository.saveAndFlush(entry);
    }

    /**
     * Возвращает список для операции "list" в рамках бизнес-логики домена.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param afterSeq граница выборки, используемая для продолжения бизнес-потока
     * @param limit ограничение размера результата бизнес-операции
     * @param includeGmOnly признак включения дополнительных данных в бизнес-ответ
     * @return результат выполнения бизнес-операции
     */
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
