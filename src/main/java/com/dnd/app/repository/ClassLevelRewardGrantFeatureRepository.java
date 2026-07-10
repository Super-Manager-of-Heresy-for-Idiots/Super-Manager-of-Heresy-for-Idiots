package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantFeatureRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantFeatureRepository extends JpaRepository<ClassLevelRewardGrantFeature, UUID> {

    List<ClassLevelRewardGrantFeature> findAllByClassFeatureId(UUID classFeatureId);
}
