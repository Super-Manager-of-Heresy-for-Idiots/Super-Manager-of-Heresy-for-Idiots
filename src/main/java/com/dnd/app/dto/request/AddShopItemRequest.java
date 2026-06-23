package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** GM stocks a merchant NPC's shop with an item template at a price and quantity. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddShopItemRequest {

    @NotNull(message = "Item template ID is required")
    private UUID itemTemplateId;

    /** Sale price in gold; when null the item template's base price is used. */
    private BigDecimal priceGold;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
