package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс BattleResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResponse {

    private UUID id;
    private UUID campaignId;
    private String name;
    private String status;
    /** Локация боя в мире кампании (WORLD_PLAN Этап 4) или null. */
    private LocationRefResponse location;
    /** Карта локации по умолчанию (external map-service id) для предвыбора или null. */
    private UUID defaultMapId;
    private Integer roundNumber;
    private Integer currentTurnIndex;
    private UUID currentCombatantId;

    // --- group preview ---
    private Integer monsterCount;
    private BigDecimal averageDanger;
    private Integer totalXp;
    private Integer overrideXp;

    private List<BattleCombatantResponse> combatants;

    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
