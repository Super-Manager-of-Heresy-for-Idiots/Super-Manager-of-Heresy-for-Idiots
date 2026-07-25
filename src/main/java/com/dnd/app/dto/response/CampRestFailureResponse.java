package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CampRestFailureResponse описывает отклонённую транзакцию отдыха одного участника привала.
 * Мастер может повторить отдых точечно только для таких персонажей.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampRestFailureResponse {

    private UUID characterId;
    private String characterName;
    private String errorCode;
    private String errorMessage;
}
