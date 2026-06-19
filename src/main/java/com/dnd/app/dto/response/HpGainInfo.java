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
public class HpGainInfo {
    private Integer hitDie;
    private Integer conModifier;
    private Integer average;
    private Integer rolledMin;
    private Integer rolledMax;
    private Integer currentMaxHp;
}
