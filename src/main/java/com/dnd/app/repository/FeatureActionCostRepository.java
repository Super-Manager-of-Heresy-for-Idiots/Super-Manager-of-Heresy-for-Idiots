package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureActionCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureActionCostRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureActionCostRepository extends JpaRepository<FeatureActionCost, UUID> {
    List<FeatureActionCost> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureActionCost> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
