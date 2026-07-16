package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO PendingResolutionResponse — «отложенный исход» заклинания у цели для окна выбора (SAVE_PROMPT).
 * Несёт скатанный урон, параметры спасброска и рекомендацию движка; ответственный за цель выбирает исход.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingResolutionResponse {

    private UUID id;
    private String spellName;
    /** Скатанный урон до митигации/спаса (весь урон при провале). */
    private int damageAmount;
    /** Название типа урона для показа (напр. «огонь»); null — нетипизированный. */
    private String damageTypeName;
    /** Урон половинится при успешном спасе (иначе снимается полностью). */
    private boolean halfOnSave;
    private Integer saveDc;
    /** Слаг характеристики спасброска (dex/con/…). */
    private String saveAbility;
    /** Рекомендация движка: SUCCESS | FAIL | null. */
    private String recommendedOutcome;
    private Integer recommendedRoll;
    private Integer recommendedSaveBonus;
}
