package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Класс MonsterSummaryResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonsterSummaryResponse {
    private UUID id;
    private String slug;
    private String name;
    private String nameRusloc;
    private String nameEngloc;
    /** Портрет монстра: прокси-URL media-ассета или null. */
    private String portraitUrl;
    /** Токен монстра для карты: прокси-URL media-ассета или null. */
    private String tokenUrl;
    private MonsterResponse.DictionaryRef size;
    private String crRating;
    private BigDecimal crValue;
    private String scope;
    private UUID homebrewId;
    private UUID campaignId;
    private Boolean isVisibleToPlayers;
    private Boolean isActive;
}
