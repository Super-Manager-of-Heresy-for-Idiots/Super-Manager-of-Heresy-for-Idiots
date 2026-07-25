package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Класс ShopSettingsResponse описывает опциональные настройки экономики торговца
 * (WORLD_PLAN Этап 5). null в поле означает «не задано» — работает прежнее поведение.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopSettingsResponse {

    /** Кошелёк торговца в золоте; null — выкуп без ограничений. */
    private BigDecimal merchantGold;

    /** Модификатор цен витрины в процентах (100 = базовые цены); null — базовые цены. */
    private Integer priceModifierPercent;
}
