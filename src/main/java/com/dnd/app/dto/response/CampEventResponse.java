package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CampEventResponse описывает запись журнала привала.
 * Скрытые заготовки мастера игрокам не отдаются.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampEventResponse {

    private UUID id;

    /** AMBUSH | ENCOUNTER | STORY | WEATHER | CUSTOM. */
    private String type;

    private String title;
    private String description;

    /** Внутриигровое время события — свободная метка мастера. */
    private String occurredLabel;

    private Boolean visibleToPlayers;

    /** Бой, созданный из засады; null, если энкаунтер не создавался. */
    private UUID battleId;

    private Instant triggeredAt;
    private Instant createdAt;
    private String createdByUsername;
}
