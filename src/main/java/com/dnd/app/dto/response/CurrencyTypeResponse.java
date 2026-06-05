package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyTypeResponse {
    private UUID id;
    private String name;
    private BigDecimal exchangeRateToGold;
    private Boolean isDefault;
}
