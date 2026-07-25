package com.dnd.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Класс UpdateShopSettingsRequest описывает запрос мастера на настройку экономики торговца
 * (WORLD_PLAN Этап 5). Обе настройки опциональны; чтобы снять ограничение, передайте
 * {@code clearMerchantGold} / {@code clearPriceModifier}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShopSettingsRequest {

    /** Кошелёк торговца в золоте; null — не менять. */
    @DecimalMin(value = "0.0", message = "merchantGold must be zero or positive")
    private BigDecimal merchantGold;

    /** Модификатор цен витрины в процентах; null — не менять. */
    @Min(value = 1, message = "priceModifierPercent must be at least 1")
    @Max(value = 1000, message = "priceModifierPercent must not exceed 1000")
    private Integer priceModifierPercent;

    /** true — снять ограничение по кошельку торговца (выкуп без лимита). */
    private Boolean clearMerchantGold;

    /** true — вернуть базовые цены (снять модификатор). */
    private Boolean clearPriceModifier;
}
