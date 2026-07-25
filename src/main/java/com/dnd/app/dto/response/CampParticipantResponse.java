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
 * Класс CampParticipantResponse описывает участника привала для экрана лагеря:
 * состояние отдыха, дозор, активность и снимок результата отдыха. Ошибка транзакции
 * приходит здесь же — отдых применяется per-character и не откатывает остальных.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampParticipantResponse {

    private UUID id;
    private UUID characterId;
    private String characterName;
    private UUID ownerId;
    private String ownerUsername;
    private String avatarUrl;

    /** Суммарный уровень и раскладка по классам для подписи карточки. */
    private Integer totalLevel;
    private List<ClassLevelResponse> classLevels;

    /** NOT_RESTED | RESTING | RESTED | PARTIAL | FAILED. */
    private String state;

    private Integer currentHp;
    private Integer maxHp;
    private Integer tempHp;

    private List<HitDiceResponse> hitDice;

    private Integer watchSlot;
    private String watchSlotLabel;

    private UUID activityId;
    private String activityName;
    private String activityIconCode;
    private String activityNote;

    /** Снимок результата последнего применённого отдыха. */
    private RestResult restResult;

    private String restErrorCode;
    private String restErrorMessage;
    private Instant restedAt;
}
