package com.dnd.app.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * Класс LoginPageStatsResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Value
@Builder
public class LoginPageStatsResponse {
    long campaignCount;
    long userCount;
    long vigilDays;
}
