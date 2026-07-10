package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.GameplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт GameplayEventRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface GameplayEventRepository extends JpaRepository<GameplayEvent, UUID> {
}
