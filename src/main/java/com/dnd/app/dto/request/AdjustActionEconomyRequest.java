package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс AdjustActionEconomyRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustActionEconomyRequest {

    @Min(0)
    @Max(20)
    private Integer actionMax;

    @Min(0)
    @Max(20)
    private Integer bonusActionMax;

    @Min(0)
    @Max(20)
    private Integer legendaryActionMax;
}
