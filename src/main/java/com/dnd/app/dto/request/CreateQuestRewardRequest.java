package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Класс CreateQuestRewardRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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
