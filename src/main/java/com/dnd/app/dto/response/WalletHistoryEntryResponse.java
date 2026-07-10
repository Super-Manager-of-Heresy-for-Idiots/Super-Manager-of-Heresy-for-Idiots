package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Класс WalletHistoryEntryResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletHistoryEntryResponse {
    private UUID id;
    private UUID currencyTypeId;
    private String currencyName;
    private BigDecimal delta;
    private BigDecimal balanceAfter;
    private String reason;
    private String performedBy;
    private Instant createdAt;
}
