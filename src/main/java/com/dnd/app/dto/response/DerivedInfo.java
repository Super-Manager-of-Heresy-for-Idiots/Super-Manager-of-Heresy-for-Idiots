package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DerivedInfo {
    private Integer proficiencyBonusBefore;
    private Integer proficiencyBonusAfter;
    // Не считаются из текущей БД (нет таблиц прогрессии) — пока null.
    private String spellSlotsGained;
    private Integer cantripsGained;
}
