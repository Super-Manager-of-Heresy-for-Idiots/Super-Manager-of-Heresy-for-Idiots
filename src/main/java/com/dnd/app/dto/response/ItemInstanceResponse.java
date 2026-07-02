package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemInstanceResponse {
    private UUID id;
    private UUID templateId;
    private String templateName;
    private String displayName;
    private String customName;
    private Integer quantity;
    private Boolean isUnique;
    private String slot;
    private String notes;
    private String rarity;
    private String itemTypeName;
    private String damageDice;
    private String damageType;
    /** Approximate unit price in gold, resolved from the backing catalog item; null when unknown. */
    private BigDecimal priceGold;
    private List<EnchantmentResponse> enchantments;
}
