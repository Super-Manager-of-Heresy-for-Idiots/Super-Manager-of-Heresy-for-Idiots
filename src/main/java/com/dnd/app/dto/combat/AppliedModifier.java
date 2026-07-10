package com.dnd.app.dto.combat;

/**
 * Запись AppliedModifier описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param value входящее значение value, используемое бизнес-сценарием
 * @param source входящее значение source, используемое бизнес-сценарием
 * @param stackKey входящее значение stack key, используемое бизнес-сценарием
 */
public record AppliedModifier(int value, String source, String stackKey) {
}
