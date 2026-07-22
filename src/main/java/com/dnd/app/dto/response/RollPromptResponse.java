package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.RollPromptStatus;
import com.dnd.app.domain.enums.RollPromptType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс RollPromptResponse описывает запрошенную мастером проверку и её результат
 * (ROLL_PROMPT). Для игрока DC скрывается, пока hideDc и бросок не совершён.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollPromptResponse {

    private UUID id;
    private UUID campaignId;
    private UUID characterId;
    private String characterName;
    /** Владелец персонажа — фронт показывает окно броска только ему. */
    private UUID ownerUserId;
    private RollPromptType rollType;
    private UUID statTypeId;
    private String statName;
    /** null для игрока, если DC скрыт и бросок ещё не совершён. */
    private Integer dc;
    private Boolean hideDc;
    private String advantageMode;
    private String description;
    private RollPromptStatus status;
    private String requestedByName;

    // --- результат ---
    private Integer rollNatural;
    private Integer rollSecond;
    private Integer modifier;
    private Integer total;
    private Boolean success;

    private Instant createdAt;
    private Instant rolledAt;
}
