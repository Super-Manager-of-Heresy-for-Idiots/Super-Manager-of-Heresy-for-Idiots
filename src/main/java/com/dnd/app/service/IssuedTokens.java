package com.dnd.app.service;

import com.dnd.app.dto.response.UserResponse;

/**
 * Запись IssuedTokens описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param accessToken входящее значение access token, используемое бизнес-сценарием
 * @param refreshToken входящее значение refresh token, используемое бизнес-сценарием
 * @param accessExpiresInMs входящее значение access expires in ms, используемое бизнес-сценарием
 * @param refreshExpiresInMs входящее значение refresh expires in ms, используемое бизнес-сценарием
 * @param user входящее значение user, используемое бизнес-сценарием
 */
public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long accessExpiresInMs,
        long refreshExpiresInMs,
        UserResponse user) {
}
