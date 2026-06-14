package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
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
public class CreateQuestRewardRequest {

    private UUID itemTemplateId;

    private Integer quantity;

    private UUID currencyTypeId;

    private BigDecimal currencyAmount;

    @Min(value = 0, message = "XP amount must not be negative")
    private Integer xpAmount;
}
