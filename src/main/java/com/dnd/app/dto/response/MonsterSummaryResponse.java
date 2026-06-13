package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonsterSummaryResponse {
    private UUID id;
    private String slug;
    private String nameRusloc;
    private String nameEngloc;
    private MonsterResponse.DictionaryRef size;
    private String crRating;
    private BigDecimal crValue;
    private String scope;
    private UUID homebrewId;
    private UUID campaignId;
    private Boolean isVisibleToPlayers;
    private Boolean isActive;
}
