package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** Outcome of a buy/sell transaction with a merchant NPC. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResultResponse {
    private UUID characterId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPriceGold;
    private BigDecimal totalPriceGold;
    /** The character's gold balance after the transaction. */
    private BigDecimal goldBalance;
}
