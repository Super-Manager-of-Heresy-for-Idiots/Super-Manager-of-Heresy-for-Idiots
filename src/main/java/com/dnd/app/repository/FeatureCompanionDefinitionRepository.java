package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureCompanionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureCompanionDefinitionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureCompanionDefinitionRepository extends JpaRepository<FeatureCompanionDefinition, UUID> {
    List<FeatureCompanionDefinition> findByFeatureRuleIdOrderBySortOrderAsc(UUID featureRuleId);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
