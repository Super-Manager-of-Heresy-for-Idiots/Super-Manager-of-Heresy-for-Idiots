package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureResourceDefinitionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureResourceDefinitionRepository extends JpaRepository<FeatureResourceDefinition, UUID> {
    List<FeatureResourceDefinition> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureResourceDefinition> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);

    @Query("SELECT DISTINCT r.resourceKey FROM FeatureResourceDefinition r "
            + "WHERE r.resourceKey IS NOT NULL ORDER BY r.resourceKey")
    List<String> findDistinctResourceKeys();
}
