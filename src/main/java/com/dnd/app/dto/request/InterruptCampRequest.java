package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.CampInterruptReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс InterruptCampRequest описывает прерывание привала мастером: причина и,
 * при наличии, событие журнала, из-за которого отряд подняли.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterruptCampRequest {

    /** AMBUSH | EVENT | MANUAL; по умолчанию MANUAL. */
    private CampInterruptReason reason;

    /** Событие журнала — причина прерывания. */
    private UUID eventId;
}
