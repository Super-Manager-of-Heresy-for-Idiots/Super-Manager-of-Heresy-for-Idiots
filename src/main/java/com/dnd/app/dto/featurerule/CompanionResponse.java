package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A character's feature companion, with computed HP/AC/attack when formulas are present. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionResponse {
    private UUID id;
    private UUID monsterId;
    private String customName;
    private String state;
    private Integer hp;
    private Integer ac;
    private Integer attackBonus;
}
