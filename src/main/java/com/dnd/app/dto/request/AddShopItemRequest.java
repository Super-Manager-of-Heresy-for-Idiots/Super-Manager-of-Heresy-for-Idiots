package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Класс AddShopItemRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddShopItemRequest {

    @NotNull(message = "Item template ID is required")
    private UUID itemTemplateId;

    /** Sale price in gold; when null the item template's base price is used. */
    private BigDecimal priceGold;

    /**
     * Сколько единиц добавить к остатку. Допускается 0 — тогда запрос лишь обновляет
     * параметры позиции (цену / базовый запас), не пополняя её.
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be zero or positive")
    private Integer quantity;

    /**
     * Базовый запас позиции для рестокинга по команде мастера (WORLD_PLAN Этап 5).
     * null — позиция не восстанавливается (прежнее поведение).
     */
    @Min(value = 0, message = "Restock quantity must be zero or positive")
    private Integer restockQuantity;

    /** true — снять базовый запас у позиции (она перестанет восстанавливаться). */
    private Boolean clearRestockQuantity;
}
