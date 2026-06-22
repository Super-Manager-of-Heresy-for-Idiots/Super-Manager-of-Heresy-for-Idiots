package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** One line in a merchant NPC's shop: an item on sale with its price (gold) and stock. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopItemResponse {
    private UUID id;
    private UUID itemTemplateId;
    private String itemName;
    private BigDecimal priceGold;
    private Integer quantity;
}
