package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantAbilityScoreRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantAbilityScoreRepository extends JpaRepository<ClassLevelRewardGrantAbilityScore, UUID> {

    List<ClassLevelRewardGrantAbilityScore> findAllByAbilityScoreId(UUID abilityScoreId);
}
