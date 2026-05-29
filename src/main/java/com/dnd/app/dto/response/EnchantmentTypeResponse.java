package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnchantmentTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private BuffDebuffResponse buffDebuff;
}
