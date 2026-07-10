package com.dnd.app.dto.combat;

import java.util.UUID;

/**
 * Запись HpChangeResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
 * @param currentHp входящее значение current hp, используемое бизнес-сценарием
 * @param tempHp входящее значение temp hp, используемое бизнес-сценарием
 * @param maxHp входящее значение max hp, используемое бизнес-сценарием
 * @param reachedZero входящее значение reached zero, используемое бизнес-сценарием
 */
public record HpChangeResult(UUID characterId, int currentHp, int tempHp, int maxHp, boolean reachedZero) {
}
