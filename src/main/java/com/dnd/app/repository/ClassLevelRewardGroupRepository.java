package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGroupRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGroupRepository extends JpaRepository<ClassLevelRewardGroup, UUID> {

    List<ClassLevelRewardGroup> findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(UUID classId);

    List<ClassLevelRewardGroup> findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(UUID classId, Integer classLevel);

    List<ClassLevelRewardGroup> findAllByClassFeatureIdOrderBySortOrderAsc(UUID classFeatureId);
}
