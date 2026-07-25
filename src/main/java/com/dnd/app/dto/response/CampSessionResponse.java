package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс CampSessionResponse описывает полное состояние привала для экрана лагеря:
 * статус и допустимые переходы, локацию с меткой безопасности, состав, расписание дозора
 * и журнал событий. Источник истины для интерфейса — этот ответ, вебсокет лишь сигнализирует
 * о необходимости его перечитать.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampSessionResponse {

    private UUID id;
    private UUID campaignId;
    private String name;
    private String description;
    private Integer dayNumber;

    /** SETTING_UP | ACTIVE | RESTING | INTERRUPTED | COMPLETED. */
    private String status;

    /** Пройденные статусы для таймлайна привала. */
    private List<String> visitedStatuses;

    /** Статусы, в которые мастер может перевести привал прямо сейчас. */
    private List<String> availableTransitions;

    /** Канонический код запущенного отдыха ({@code long_rest} / {@code short_rest}). */
    private String restType;

    private Boolean applyPartialRest;

    private LocationRefResponse location;

    /** SAFE | RISKY | DANGEROUS — метка локации; null, если привал вне локации. */
    private String restSafety;

    /** AMBUSH | EVENT | MANUAL — причина прерывания. */
    private String interruptReason;

    private UUID interruptEventId;

    /** Бой, созданный прервавшим привал событием. */
    private UUID interruptBattleId;

    private Integer watchSlotCount;
    private List<CampWatchSlotResponse> watchSchedule;

    private List<CampParticipantResponse> participants;
    private List<CampEventResponse> events;

    private UUID createdById;
    private String createdByUsername;

    private Instant startedAt;
    private Instant restStartedAt;
    private Instant restCompletedAt;
    private Instant interruptedAt;
    private Instant endedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
