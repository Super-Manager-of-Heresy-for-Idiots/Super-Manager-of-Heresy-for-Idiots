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
public class QuestRewardResponse {

    private UUID id;
    private UUID itemTemplateId;
    private String itemTemplateName;
    private Integer quantity;
    private UUID currencyTypeId;
    private String currencyTypeName;
    private BigDecimal currencyAmount;
    private Integer xpAmount;
}
