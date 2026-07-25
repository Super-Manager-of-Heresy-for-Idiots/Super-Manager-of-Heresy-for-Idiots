package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.CampEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс CreateCampEventRequest описывает запись в журнал привала. Засада (AMBUSH)
 * дополнительно может создать бой: тогда привал переходит в INTERRUPTED,
 * а в журнале появляется ссылка на созданный энкаунтер.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampEventRequest {

    @NotNull(message = "Event type is required")
    private CampEventType type;

    @NotBlank(message = "Event title is required")
    @Size(max = 150, message = "Event title must not exceed 150 characters")
    private String title;

    private String description;

    /** Внутриигровое время события — свободная метка мастера («01:20»). */
    @Size(max = 20, message = "Occurred label must not exceed 20 characters")
    private String occurredLabel;

    /**
     * Срабатывает ли событие сразу. false — мастер сохраняет скрытую заготовку,
     * которую позже триггерит вручную.
     */
    private Boolean triggerNow;

    /** Видно ли событие игрокам после срабатывания. */
    private Boolean visibleToPlayers;

    /** Создавать ли бой из засады. Учитывается только для типа AMBUSH. */
    private Boolean createBattle;

    @Size(max = 120, message = "Battle name must not exceed 120 characters")
    private String battleName;

    /** Черновик энкаунтера: монстры бестиария и их количество. */
    @Valid
    private List<AddBattleMonstersRequest.MonsterEntry> monsters;
}
