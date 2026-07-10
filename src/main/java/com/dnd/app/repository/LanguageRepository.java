package com.dnd.app.repository;

import com.dnd.app.domain.Language;

/**
 * Контракт LanguageRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface LanguageRepository extends DictionaryRepository<Language> {
}
