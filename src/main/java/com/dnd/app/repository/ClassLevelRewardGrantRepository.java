package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantRepository extends JpaRepository<ClassLevelRewardGrant, UUID> {

    List<ClassLevelRewardGrant> findAllByRewardGroupIdOrderBySortOrderAsc(UUID groupId);

    List<ClassLevelRewardGrant> findAllByRewardOptionIdOrderBySortOrderAsc(UUID optionId);

    List<ClassLevelRewardGrant> findAllByGrantType(String grantType);
}
