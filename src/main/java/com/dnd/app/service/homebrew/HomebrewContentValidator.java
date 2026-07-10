package com.dnd.app.service.homebrew;

import com.dnd.app.dto.response.ContentSummaryDto;

import java.util.UUID;

/**
 * Контракт HomebrewContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HomebrewContentValidator {
    String getSupportedType();
    void validateExists(UUID contentId);
    ContentSummaryDto summarize(UUID contentId);
    UUID getOwnerId(UUID contentId);
}
