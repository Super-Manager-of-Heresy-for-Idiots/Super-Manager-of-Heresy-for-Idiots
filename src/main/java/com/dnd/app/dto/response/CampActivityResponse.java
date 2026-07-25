package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CampActivityResponse описывает даунтайм-активность справочника привала.
 * Механических автоэффектов у активности нет — награды выдаёт мастер вручную.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampActivityResponse {

    private UUID id;

    /** null для системной активности. */
    private UUID campaignId;

    /** SYSTEM | CUSTOM — вычисляется из принадлежности кампании. */
    private String kind;

    private String name;
    private String description;
    private String iconCode;

    /** Подсказка мастеру, какую проверку уместно запросить. */
    private String skillCheckRef;
}
