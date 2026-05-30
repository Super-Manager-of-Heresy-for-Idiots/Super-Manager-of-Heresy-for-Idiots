package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class ModifyCurrencyRequest {

    @NotNull(message = "Currency type ID is required")
    private UUID currencyTypeId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
