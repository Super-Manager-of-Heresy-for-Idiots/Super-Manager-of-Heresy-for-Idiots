package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CampRestSummaryResponse описывает итог группового отдыха: обновлённое состояние
 * привала, число успешных транзакций и отдельный список ошибок. Откатов нет — сбой одного
 * участника не отменяет отдых остальных.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampRestSummaryResponse {

    private UUID campId;

    /** Канонический код применённого отдыха ({@code long_rest} / {@code short_rest}). */
    private String restType;

    /** Применён ли частичный отдых (решение мастера после прерывания). */
    private Boolean partial;

    private Integer restedCount;
    private Integer failedCount;
    private Integer skippedCount;

    /** Участники, у которых транзакция отдыха не прошла. */
    private List<CampRestFailureResponse> failures;

    /** Полное состояние привала после отдыха. */
    private CampSessionResponse camp;
}
