package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CampWatchSlotResponse описывает слот дозора привала: номер, метку времени
 * и назначенного в него персонажа. Пустой слот приходит без characterId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampWatchSlotResponse {

    private Integer slot;

    /** Свободная метка времени дозора («20:00 — 22:30»); задаётся мастером. */
    private String label;

    private UUID characterId;

    private String characterName;
}
