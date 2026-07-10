package com.dnd.app.repository;

import com.dnd.app.domain.Habitat;

/**
 * Контракт HabitatRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HabitatRepository extends DictionaryRepository<Habitat> {
}
