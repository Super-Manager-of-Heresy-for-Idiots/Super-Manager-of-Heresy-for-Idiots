package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantFeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantFeatRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantFeatRepository extends JpaRepository<ClassLevelRewardGrantFeat, UUID> {

    List<ClassLevelRewardGrantFeat> findAllByFeatId(UUID featId);
}
